import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档-代码一致性自检。
 *
 * 扫描 docs/components、docs/framework 与 README.md（docs/superpowers 为历史过程稿，不校验），
 * 对每篇 markdown 做四件事：
 *   1. 抽取 ```java 代码块。
 *      - 若是「语句型示例」（可独立复制运行，如组件文档里的用法片段）→ 收集 import 后合成包装类，
 *        用 JDK 编译器对照 out/ 真正编译，任何引用已删除类 / 签名漂移都会编译失败。
 *      - 若是「类/方法摘录」（带 class / @Override / 方法签名，如框架文档里的片段）→ 不编译，
 *        仅做 FQCN 存在性校验（避免把教学片段当完整代码要求编译）。
 *   2. 校验 ``` 块中 `java -cp out <类>` 命令行引用的类在 src/ 中真实存在。
 *   3. 校验 ```java 块里出现的 org.swelement.* 类型在 src/ 中真实存在（覆盖摘录型块）。
 *   4. 校验 markdown 图片/文档相对链接（![..](..)、[..](..)）指向的文件真实存在。
 *
 * 用法：java -cp out DocSnippetCheck [项目根目录]
 * 退出码：0 = 全部通过；1 = 存在漂移；2 = 环境错误（未在 JDK 上运行）。
 */
public class DocSnippetCheck {

    private static final Pattern IMPORT_LINE = Pattern.compile("^\\s*import\\s+[\\w.*]+\\s*;\\s*$");
    private static final Pattern CLASS_DECL = Pattern.compile("(?m)^\\s*(public\\s+|final\\s+|abstract\\s+)*class\\s+(\\w+)");
    private static final Pattern JAVA_CMD = Pattern.compile("\\bjava\\s+(?:-[\\w.]+\\s+)*-cp\\s+out\\s+([\\w.]+)");
    private static final Pattern MD_LINK = Pattern.compile("!??\\[[^\\]]*\\]\\(([^)]+)\\)");
    private static final Pattern FQCN = Pattern.compile("org\\.swelement\\.[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*");
    private static final Pattern METHOD_DECL = Pattern.compile(
            "^(public|protected|private|static|final|abstract)\\b.*\\w+\\s*\\([^)]*\\)\\s*\\{?\\s*$");

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        Path outDir = root.resolve("out");
        if (!Files.isDirectory(outDir)) {
            System.err.println("ERROR: 未找到 out/ 目录，请先运行 build.bat 编译");
            System.exit(2);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("ERROR: 需要在 JDK（而非 JRE）上运行，javax.tools.JavaCompiler 不可用");
            System.exit(2);
        }

        List<Path> docs = new ArrayList<>();
        collectMd(root.resolve("docs/components"), docs);
        collectMd(root.resolve("docs/framework"), docs);
        Path readme = root.resolve("README.md");
        if (Files.exists(readme)) docs.add(readme);
        if (docs.isEmpty()) {
            System.err.println("ERROR: 未找到任何待校验文档");
            System.exit(2);
        }

        Path genDir = outDir.resolve("doccheck");
        deleteRecursively(genDir);
        Files.createDirectories(genDir);

        for (Path doc : docs) {
            checkDoc(root, doc, outDir, genDir, compiler);
        }
        deleteRecursively(genDir);

        if (failures > 0) {
            System.err.println("DocSnippetCheck: FAILED, " + failures + " 处漂移");
            System.exit(1);
        }
        System.out.println("DocSnippetCheck OK (" + docs.size() + " 篇文档)");
    }

    private static void checkDoc(Path root, Path doc, Path outDir, Path genDir, JavaCompiler compiler) {
        List<String> lines;
        try {
            lines = Files.readAllLines(doc, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail(doc, 0, "无法读取: " + e.getMessage());
            return;
        }

        // 1) 抽取 ```java 块
        List<Block> blocks = new ArrayList<>();
        Set<String> imports = new LinkedHashSet<>();
        boolean inJava = false;
        List<String> cur = null;
        int curStart = 0;
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (!inJava && t.equalsIgnoreCase("```java")) {
                inJava = true; cur = new ArrayList<>(); curStart = i + 1;
            } else if (inJava && t.equals("```")) {
                inJava = false;
                List<String> body = new ArrayList<>();
                for (String bl : cur) {
                    if (IMPORT_LINE.matcher(bl).matches()) imports.add(bl.trim());
                    else body.add(bl);
                }
                if (!body.isEmpty()) blocks.add(new Block(body, curStart));
                cur = null;
            } else if (inJava) {
                cur.add(lines.get(i));
            }
        }

        // 2) 编译 / FQCN 校验 java 代码块
        if (!blocks.isEmpty()) {
            boolean compile = !doc.toString().replace('\\', '/').contains("/docs/framework/");
            compileBlocks(root, doc, blocks, imports, outDir, genDir, compiler, compile);
        }

        // 3) 校验 java -cp out <类> 命令
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = JAVA_CMD.matcher(lines.get(i));
            while (m.find()) {
                String cls = m.group(1);
                Path src = root.resolve("src").resolve(Paths.get(cls.replace('.', File.separatorChar) + ".java"));
                if (!Files.exists(src)) {
                    fail(doc, i + 1, "命令引用的类不存在: " + cls);
                }
            }
        }

        // 4) 校验相对链接（图片与文档）
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = MD_LINK.matcher(lines.get(i));
            while (m.find()) {
                String target = m.group(1).trim();
                int sp = target.indexOf(' ');
                if (sp >= 0) target = target.substring(0, sp); // 去掉可选 title
                if (target.isEmpty() || target.startsWith("http://") || target.startsWith("https://")
                        || target.startsWith("#") || target.startsWith("mailto:")) continue;
                int hash = target.indexOf('#');
                if (hash >= 0) target = target.substring(0, hash);
                if (target.isEmpty()) continue;
                Path resolved = doc.getParent().resolve(target).normalize();
                if (!Files.exists(resolved)) {
                    fail(doc, i + 1, "链接目标不存在: " + m.group(1).trim());
                }
            }
        }
    }

    private static void compileBlocks(Path root, Path doc, List<Block> blocks, Set<String> imports,
                                       Path outDir, Path genDir, JavaCompiler compiler, boolean compile) {
        // 框架文档里的块多为依赖基类上下文的片段，无法独立编译，仅做 FQCN 存在性校验。
        if (!compile) {
            for (Block b : blocks) checkFqcn(root, doc, b);
            return;
        }
        String base = "DocSnippet_" + doc.getFileName().toString().replaceAll("[^\\w]", "_");
        StringBuilder sb = new StringBuilder();
        List<String> origin = new ArrayList<>();
        for (String imp : imports) { sb.append(imp).append('\n'); origin.add(doc + ":imports"); }
        sb.append("import javax.swing.*;\n"); origin.add(doc + ":auto-import");
        sb.append("import java.awt.*;\n");   origin.add(doc + ":auto-import");
        sb.append("public class ").append(base).append(" {\n"); origin.add(doc + ":wrapper");

        List<JavaFileObject> units = new ArrayList<>();
        int n = 0;
        for (Block b : blocks) {
            if (isCompileCandidate(b.lines)) {
                sb.append("    public void snippet_").append(n++).append("() throws Exception {\n");
                origin.add(doc + ":" + b.startLine);
                for (int k = 0; k < b.lines.size(); k++) {
                    sb.append(b.lines.get(k)).append('\n');
                    origin.add(doc + ":" + (b.startLine + k));
                }
                sb.append("    }\n"); origin.add(doc + ":wrapper");
            } else {
                // 摘录型块：仅 FQCN 存在性校验
                checkFqcn(root, doc, b);
            }
        }
        sb.append("}\n"); origin.add(doc + ":wrapper");
        Path wrapper = genDir.resolve(base + ".java");
        write(wrapper, sb.toString());
        units.add(toFileObject(wrapper));

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
        List<String> options = new ArrayList<>();
        options.add("-cp"); options.add(outDir.toString());
        options.add("-d"); options.add(genDir.toString());
        options.add("-nowarn");
        Boolean ok = compiler.getTask(null, fm, diagnostics, options, null, units).call();
        if (!Boolean.TRUE.equals(ok)) {
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() != Diagnostic.Kind.ERROR) continue;
                String where;
                if (d.getSource() != null && d.getSource().getName().endsWith(base + ".java")) {
                    int li = (int) d.getLineNumber() - 1;
                    where = (li >= 0 && li < origin.size()) ? origin.get(li) : doc + ":?";
                } else {
                    where = doc + "(独立类块)";
                }
                fail(null, 0, where + " 编译失败: " + d.getMessage(null));
            }
        }
        try { fm.close(); } catch (IOException ignore) {}
    }

    /** 摘录型代码块：校验其中出现的 org.swelement.* 类型是否真实存在。 */
    private static void checkFqcn(Path root, Path doc, Block b) {
        StringBuilder all = new StringBuilder();
        for (String l : b.lines) all.append(l).append('\n');
        Matcher m = FQCN.matcher(all);
        Set<String> seen = new LinkedHashSet<>();
        while (m.find()) seen.add(m.group(0));
        for (String fqcn : seen) {
            Path p = resolveType(root, fqcn);
            if (p == null) fail(doc, b.startLine, "引用了不存在的类型: " + fqcn);
        }
    }

    private static Path resolveType(Path root, String fqcn) {
        String[] parts = fqcn.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(File.separatorChar);
            sb.append(parts[i]);
        }
        Path p = root.resolve("src").resolve(sb.toString() + ".java");
        return Files.exists(p) ? p : null;
    }

    /** 语句型示例（可独立编译）：不含 class 声明、@Override、方法签名。 */
    private static boolean isCompileCandidate(List<String> body) {
        for (String l : body) {
            String t = l.trim();
            if (t.isEmpty() || t.startsWith("//") || t.startsWith("import ")) continue;
            if (CLASS_DECL.matcher(t).find()) return false;
            if (t.startsWith("@Override")) return false;
            if (METHOD_DECL.matcher(t).matches()) return false;
        }
        return true;
    }

    private static void collectMd(Path dir, final List<Path> out) throws IOException {
        if (!Files.isDirectory(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".md")) out.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void fail(Path doc, int line, String msg) {
        failures++;
        if (doc != null) System.err.println("FAIL " + doc + ":" + line + " " + msg);
        else System.err.println("FAIL " + msg);
    }

    private static void write(Path f, String content) {
        try { Files.write(f, content.getBytes(StandardCharsets.UTF_8)); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    private static JavaFileObject toFileObject(Path f) {
        return new javax.tools.SimpleJavaFileObject(f.toUri(), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
            }
        };
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d); return FileVisitResult.CONTINUE;
            }
        });
    }

    private static class Block {
        final List<String> lines;
        final int startLine;
        Block(List<String> lines, int startLine) { this.lines = lines; this.startLine = startLine; }
    }
}

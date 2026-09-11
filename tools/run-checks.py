"""run-checks.bat 的 Python 等价运行器（本机沙箱用）。

用途：本沙箱禁止调用 cmd.exe（run-checks.bat 跑不了），且 coreutils 缺 `timeout`，
无法给批量自检逐条兜底超时。本脚本直接解析 run-checks.bat 里的自检命令逐条执行，
每条带 120s 超时，汇总 PASS/FAIL —— 与 run-checks.bat 结果等价。

用法：
    python tools/run-checks.py            # 全量
    python tools/run-checks.py --limit 3  # 只跑前 3 条（冒烟）

JDK 路径默认取 C:\\Program Files\\Java\\jdk1.8.0_311（与 build.bat 一致），
可用环境变量 JAVA8_HOME 覆盖。
"""
import os
import shlex
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BAT = os.path.join(ROOT, "run-checks.bat")
JAVA8 = os.environ.get("JAVA8_HOME", r"C:\Program Files\Java\jdk1.8.0_311")
JAVA = os.path.join(JAVA8, "bin", "java.exe")
JAVAC = os.path.join(JAVA8, "bin", "javac.exe")

limit = None
if "--limit" in sys.argv:
    limit = int(sys.argv[sys.argv.index("--limit") + 1])

with open(BAT, encoding="utf-8", errors="replace") as f:
    lines = f.read().splitlines()

cmds = []
for line in lines:
    s = line.strip()
    if '"%JAVAC%"' not in s and '"%JRUN%"' not in s:
        continue
    # 去掉 || 失败计数 与 >nul 重定向尾巴
    s = s.split("||")[0].split(">")[0].strip()
    # 反斜杠转正斜杠，posix shlex 才能正确分词（Java 在 Windows 接受 /）
    s = s.replace("\\", "/")
    s = s.replace('"%JAVAC%"', '"' + JAVAC.replace("\\", "/") + '"')
    s = s.replace('"%JRUN%"', '"' + JAVA.replace("\\", "/") + '"')
    cmds.append(shlex.split(s, posix=True))

if not os.path.exists(JAVA):
    print("ERROR: java not found at " + JAVA, flush=True)
    sys.exit(2)

print("total commands: %d" % len(cmds), flush=True)
if limit:
    cmds = cmds[:limit]

failed = []
for i, args in enumerate(cmds, 1):
    label = " ".join(args[2:]) if len(args) > 2 else " ".join(args)
    try:
        p = subprocess.run(args, cwd=ROOT, capture_output=True, timeout=120,
                           encoding="utf-8", errors="replace")
        rc, out, err = p.returncode, (p.stdout or "").strip(), (p.stderr or "").strip()
    except subprocess.TimeoutExpired:
        rc, out, err = 124, "", "TIMEOUT(120s)"
    print("[%d/%d] %s  %s" % (i, len(cmds), "PASS" if rc == 0 else "FAIL", label), flush=True)
    if rc != 0:
        failed.append((i, label, rc))
        print("      rc=%d out=%s err=%s" % (rc, out[-500:], err[-500:]), flush=True)

print("=" * 48, flush=True)
if not failed:
    print("ALL %d CHECKS PASSED" % len(cmds), flush=True)
    sys.exit(0)
print("%d of %d CHECKS FAILED" % (len(failed), len(cmds)), flush=True)
for i, label, rc in failed:
    print("  FAIL [%d] rc=%d %s" % (i, rc, label), flush=True)
sys.exit(1)

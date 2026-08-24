import org.swelement.ui.AstCheckbox;
import org.swelement.ui.AstMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 * 探针：验证「快速点击失效」在 AstCheckbox 与 AstMenu 上是否为同一机制。
 * 不依赖真实鼠标，直接派发 AWT 事件序列，观察状态是否变更。
 */
public class RapidClickProbe {
    private static int seq = 0;

    private static MouseEvent me(Component c, int id, int x, int y) {
        return new MouseEvent(c, id, System.currentTimeMillis() + (seq++),
                InputEvent.BUTTON1_MASK, x, y, 1, false, MouseEvent.BUTTON1);
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame f = new JFrame();
            JPanel p = new JPanel(new FlowLayout());
            AstCheckbox c1 = new AstCheckbox("A");
            AstCheckbox c2 = new AstCheckbox("B");
            AstCheckbox c3 = new AstCheckbox("C");
            p.add(c1);
            p.add(c2);
            p.add(c3);
            f.setContentPane(p);
            f.pack();

            System.out.println("=== CHECKBOX ===");

            // 对照组：干净的 按下 -> 释放（指针未离开）
            c1.dispatchEvent(me(c1, MouseEvent.MOUSE_ENTERED, 5, 10));
            c1.dispatchEvent(me(c1, MouseEvent.MOUSE_PRESSED, 5, 10));
            c1.dispatchEvent(me(c1, MouseEvent.MOUSE_RELEASED, 5, 10));
            System.out.println("[对照] enter,press,release          -> selected=" + c1.isSelected() + "   期望 true");

            // 场景1：按下与释放之间指针移出（快速挪向下一个勾选框）
            c2.dispatchEvent(me(c2, MouseEvent.MOUSE_ENTERED, 5, 10));
            c2.dispatchEvent(me(c2, MouseEvent.MOUSE_PRESSED, 5, 10));
            c2.dispatchEvent(me(c2, MouseEvent.MOUSE_EXITED, 90, 10));
            c2.dispatchEvent(me(c2, MouseEvent.MOUSE_RELEASED, 90, 10));
            System.out.println("[场景1] press,EXITED,release        -> selected=" + c2.isSelected() + "   为 false 即命中缺陷");

            // 场景2：按下与释放之间只发生移动（未越界）
            c3.dispatchEvent(me(c3, MouseEvent.MOUSE_ENTERED, 5, 10));
            c3.dispatchEvent(me(c3, MouseEvent.MOUSE_PRESSED, 5, 10));
            c3.dispatchEvent(me(c3, MouseEvent.MOUSE_DRAGGED, 8, 10));
            c3.dispatchEvent(me(c3, MouseEvent.MOUSE_RELEASED, 8, 10));
            System.out.println("[场景2] press,DRAGGED(内),release   -> selected=" + c3.isSelected() + "   期望 true");

            System.out.println();
            System.out.println("=== MENU ===");

            final boolean[] fired = {false, false};
            AstMenu m = new AstMenu();
            m.addMenuItem("首页", () -> fired[0] = true);
            m.addMenuItem("产品", () -> fired[1] = true);
            JFrame f2 = new JFrame();
            JPanel p2 = new JPanel(new BorderLayout());
            p2.add(m, BorderLayout.NORTH);
            f2.setContentPane(p2);
            f2.pack();

            // 场景3：只有 按下 -> 释放，没有 MOUSE_CLICKED（指针在按下/释放之间移动时 AWT 就是这样）
            m.dispatchEvent(me(m, MouseEvent.MOUSE_PRESSED, 10, 10));
            m.dispatchEvent(me(m, MouseEvent.MOUSE_RELEASED, 10, 10));
            System.out.println("[场景3] press,release 无 clicked    -> 条目触发=" + fired[0] + "   为 false 即命中缺陷");

            // 场景4：补上 MOUSE_CLICKED
            m.dispatchEvent(me(m, MouseEvent.MOUSE_CLICKED, 10, 10));
            System.out.println("[场景4] 补发 clicked                -> 条目触发=" + fired[0] + "   期望 true");

            f.dispose();
            f2.dispose();
        });
        System.exit(0);
    }
}

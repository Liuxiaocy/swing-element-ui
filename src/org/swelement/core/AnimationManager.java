package org.swelement.core;

import javax.swing.JComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * 动画管理器，统一管理组件的所有命名动画。
 * <p>
 * 每个组件持有一个 AnimationManager 实例，通过名称注册和控制多个动画。
 * 所有动画更新都会触发所属组件的 repaint()。
 */
public class AnimationManager {
    /** 悬停动画名称常量 */
    public static final String HOVER = "hover";
    /** 聚焦动画名称常量 */
    public static final String FOCUS = "focus";
    /** 激活动画名称常量 */
    public static final String ACTIVE = "active";
    /** 按下动画名称常量 */
    public static final String PRESS = "press";
    /** 打开动画名称常量 */
    public static final String OPEN = "open";
    /** 关闭动画名称常量 */
    public static final String CLOSE = "close";
    /** 选中动画名称常量 */
    public static final String SELECTED = "selected";

    private final JComponent owner;
    private final Map<String, Animator> animations;
    private final Map<String, Float> progress;
    private boolean disposed;

    /**
     * 创建动画管理器。
     *
     * @param owner 所属组件，不能为 null
     * @throws IllegalArgumentException 如果 owner 为 null
     */
    public AnimationManager(JComponent owner) {
        if (owner == null) {
            throw new IllegalArgumentException("owner cannot be null");
        }
        this.owner = owner;
        this.animations = new HashMap<String, Animator>();
        this.progress = new HashMap<String, Float>();
        this.disposed = false;
    }

    /**
     * 注册命名动画，返回创建的 Animator。
     * <p>
     * 动画回调会自动更新进度映射并触发 owner.repaint()。
     * 如果名称已存在，将覆盖原有动画。
     *
     * @param name       动画名称
     * @param durationMs 动画持续时间（毫秒）
     * @param easing     缓动函数
     * @return 创建的 Animator 实例
     */
    public Animator register(final String name, int durationMs, Easing easing) {
        Animator anim = new Animator(durationMs, easing, new Animator.Listener() {
            public void update(float value) {
                progress.put(name, value);
                owner.repaint();
            }
        });
        animations.put(name, anim);
        if (!progress.containsKey(name)) {
            progress.put(name, 0f);
        }
        return anim;
    }

    /**
     * 获取指定名称的动画器。
     *
     * @param name 动画名称
     * @return 动画器实例，未注册返回 null
     */
    public Animator get(String name) {
        return animations.get(name);
    }

    /**
     * 获取指定动画的当前进度值。
     *
     * @param name 动画名称
     * @return 进度值 [0, 1]，未注册返回 0
     */
    public float getProgress(String name) {
        Float p = progress.get(name);
        return p != null ? p : 0f;
    }

    /**
     * 判断是否存在指定名称的动画。
     *
     * @param name 动画名称
     * @return true 表示已注册
     */
    public boolean has(String name) {
        return animations.containsKey(name);
    }

    /**
     * 驱动动画到 1（进入状态）。
     * 从当前进度开始过渡到 1。
     *
     * @param name 动画名称
     */
    public void start(String name) {
        Animator anim = animations.get(name);
        if (anim != null) {
            float current = getProgress(name);
            anim.go(current, 1f);
        }
    }

    /**
     * 驱动动画到 0（退出状态）。
     * 从当前进度开始过渡到 0。
     *
     * @param name 动画名称
     */
    public void stop(String name) {
        Animator anim = animations.get(name);
        if (anim != null) {
            float current = getProgress(name);
            anim.go(current, 0f);
        }
    }

    /**
     * 驱动动画从 from 到 to。
     *
     * @param name 动画名称
     * @param from 起始值
     * @param to   结束值
     */
    public void go(String name, float from, float to) {
        Animator anim = animations.get(name);
        if (anim != null) {
            anim.go(from, to);
        }
    }

    /**
     * 立即设置进度（不带动画，停止当前动画）。
     * 值会自动钳制到 [0, 1] 范围。
     *
     * @param name  动画名称
     * @param value 进度值
     */
    public void setProgress(String name, float value) {
        float clamped = Math.max(0f, Math.min(1f, value));
        Animator anim = animations.get(name);
        if (anim != null) {
            anim.stop();
        }
        progress.put(name, clamped);
        owner.repaint();
    }

    /**
     * 停止所有动画。
     */
    public void stopAll() {
        for (Animator anim : animations.values()) {
            anim.stop();
        }
    }

    /**
     * 销毁：停止所有动画并清空资源。
     * 调用后所有动画被清除，getProgress 返回 0。
     */
    public void dispose() {
        stopAll();
        animations.clear();
        progress.clear();
        disposed = true;
    }

    // === 自检 ===

    /**
     * 自检方法，验证 AnimationManager 的各项功能。
     * 仅在 -ea 开启时生效。
     */
    static void selfCheck() {
        JComponent comp = new javax.swing.JPanel();
        AnimationManager mgr = new AnimationManager(comp);

        // 基本操作：register / has / get / getProgress
        assert !mgr.has("test") : "should not have unregistered animation";
        assert mgr.get("test") == null : "get should return null for unregistered";
        assert mgr.getProgress("test") == 0f : "getProgress should return 0 for unregistered";

        Animator anim = mgr.register("test", 200, Easing::linear);
        assert anim != null : "register should return Animator";
        assert mgr.has("test") : "should have registered animation";
        assert mgr.get("test") == anim : "get should return registered Animator";
        assert mgr.getProgress("test") == 0f : "initial progress should be 0";

        // setProgress 立即生效
        mgr.setProgress("test", 0.5f);
        assert mgr.getProgress("test") == 0.5f : "setProgress should take effect immediately";

        // setProgress 钳制 [0,1]
        mgr.setProgress("test", -0.5f);
        assert mgr.getProgress("test") == 0f : "setProgress should clamp to 0";
        mgr.setProgress("test", 1.5f);
        assert mgr.getProgress("test") == 1f : "setProgress should clamp to 1";

        // 注册多个动画
        mgr.register("hover", 150, Easing::easeOut);
        mgr.register("focus", 100, Easing::easeInOut);
        assert mgr.has("hover") : "should have hover";
        assert mgr.has("focus") : "should have focus";
        assert mgr.has("test") : "should still have test";

        // stopAll 停止所有
        mgr.setProgress("hover", 0.8f);
        mgr.setProgress("focus", 0.6f);
        mgr.stopAll();
        // stopAll 只停止动画，不改变进度值
        assert mgr.getProgress("hover") == 0.8f : "stopAll should not change progress";
        assert mgr.getProgress("focus") == 0.6f : "stopAll should not change progress";

        // dispose 清除所有
        mgr.dispose();
        assert !mgr.has("test") : "dispose should clear test";
        assert !mgr.has("hover") : "dispose should clear hover";
        assert !mgr.has("focus") : "dispose should clear focus";
        assert mgr.getProgress("test") == 0f : "getProgress should return 0 after dispose";

        // null owner 抛 IllegalArgumentException
        try {
            new AnimationManager(null);
            assert false : "should have thrown IllegalArgumentException for null owner";
        } catch (IllegalArgumentException expected) {
            // ok
        }

        System.out.println("AnimationManager self-check OK");
    }

    /**
     * 主方法，运行自检。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        selfCheck();
    }
}

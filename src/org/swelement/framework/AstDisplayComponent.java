package org.swelement.framework;

import java.awt.Cursor;

/**
 * 展示组件基类。
 * <p>
 * 继承自 {@link AstAbstractComponent}，用于纯展示型组件（如 Badge、Tag 等）。
 * <p>
 * 与 {@link AstInteractiveComponent} 的区别：
 * <ul>
 *   <li>默认光标为默认箭头（非手型）</li>
 *   <li>不自动注册 hover/active/focus 动画</li>
 *   <li>不安装交互事件监听器</li>
 * </ul>
 * <p>
 * 子类如需要特定交互能力，可自行注册动画和监听。
 */
public abstract class AstDisplayComponent extends AstAbstractComponent {

    /**
     * 初始化展示组件：设置默认光标（箭头）。
     */
    @Override
    protected void initComponent() {
        super.initComponent();
        setCursor(Cursor.getDefaultCursor());
    }
}

package org.swelement.framework;

import org.swelement.core.AnimationManager;
import org.swelement.core.Easing;

import java.awt.Cursor;
import java.awt.ItemSelectable;
import java.awt.event.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

/**
 * 交互组件基类。
 * <p>
 * 继承自 {@link AstAbstractComponent}，自动管理三种标准交互状态：
 * <ul>
 *   <li>hover（悬停）— 鼠标进入/离开</li>
 *   <li>active（激活/按下）— 鼠标按下/释放</li>
 *   <li>focus（聚焦）— 键盘焦点获得/失去</li>
 * </ul>
 * <p>
 * 每种状态对应一个同名动画，状态切换时自动驱动动画过渡。
 * 子类可通过 {@link #hoverProgress()}、{@link #activeProgress()}、{@link #focusProgress()}
 * 获取当前动画进度，并在绘制时使用。
 * <p>
 * 仅在组件启用（isEnabled()）时响应交互事件。
 */
public abstract class AstInteractiveComponent extends AstAbstractComponent
        implements ItemSelectable {

    /** 是否处于悬停状态 */
    private boolean hovering = false;
    /** 是否处于按下状态 */
    private boolean pressing = false;
    /** 是否处于聚焦状态 */
    private boolean focused = false;
    /** 是否处于选中状态 */
    private boolean selected = false;
    /** sticky 行为：鼠标按下标记（按下后移出再释放仍切换） */
    private boolean pressStarted = false;
    /** 选中状态监听器列表 */
    private final EventListenerList itemListenerList = new EventListenerList();

    /**
     * 初始化交互组件：注册三个标准动画、安装事件监听、设置手型光标。
     */
    @Override
    protected void initComponent() {
        super.initComponent();
        // 注册三个标准动画
        anim.register(AnimationManager.HOVER, 200, Easing::easeInOut);
        anim.register(AnimationManager.ACTIVE, 120, Easing::easeInOut);
        anim.register(AnimationManager.FOCUS, 200, Easing::easeInOut);
        anim.register(AnimationManager.SELECTED, 200, Easing::easeInOut);
        // 安装事件监听
        installInteractionListeners();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * 安装交互事件监听器（鼠标、焦点）。
     */
    private void installInteractionListeners() {
        // 鼠标监听：驱动 hover 和 active 状态
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isEnabled()) return;
                hovering = true;
                anim.start(AnimationManager.HOVER);
                onHoverChanged(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isEnabled()) return;
                hovering = false;
                pressing = false;
                anim.stop(AnimationManager.HOVER);
                anim.stop(AnimationManager.ACTIVE);
                onHoverChanged(false);
                onActiveChanged(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                if (SwingUtilities.isLeftMouseButton(e)) {
                    pressing = true;
                    pressStarted = true;
                    anim.start(AnimationManager.ACTIVE);
                    onActiveChanged(true);
                    // 请求焦点
                    requestFocusInWindow();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isEnabled()) return;
                if (pressing) {
                    pressing = false;
                    anim.stop(AnimationManager.ACTIVE);
                    onActiveChanged(false);
                }
                // sticky 行为：只要 pressStarted 为 true，释放时切换选中状态（仅切换模式下）
                if (pressStarted) {
                    pressStarted = false;
                    if (isToggleMode()) {
                        setSelected(!selected);
                    }
                    onActionPerformed();
                }
            }
        });

        // 焦点监听：驱动 focus 状态
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!isEnabled()) return;
                focused = true;
                anim.start(AnimationManager.FOCUS);
                onFocusChanged(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!isEnabled()) return;
                focused = false;
                anim.stop(AnimationManager.FOCUS);
                onFocusChanged(false);
            }
        });
    }

    // ==================== 便捷进度获取 ====================

    /**
     * 获取悬停动画当前进度。
     *
     * @return 进度值 [0, 1]
     */
    protected float hoverProgress() {
        return anim.getProgress(AnimationManager.HOVER);
    }

    /**
     * 获取激活动画当前进度。
     *
     * @return 进度值 [0, 1]
     */
    protected float activeProgress() {
        return anim.getProgress(AnimationManager.ACTIVE);
    }

    /**
     * 获取聚焦动画当前进度。
     *
     * @return 进度值 [0, 1]
     */
    protected float focusProgress() {
        return anim.getProgress(AnimationManager.FOCUS);
    }

    /**
     * 获取选中动画当前进度。
     *
     * @return 进度值 [0, 1]
     */
    protected float selectedProgress() {
        return anim.getProgress(AnimationManager.SELECTED);
    }

    // ==================== 状态查询 ====================

    /**
     * 查询是否处于悬停状态。
     *
     * @return true 表示悬停中
     */
    protected boolean isHovering() {
        return hovering;
    }

    /**
     * 查询是否处于按下状态。
     *
     * @return true 表示按下中
     */
    protected boolean isPressing() {
        return pressing;
    }

    /**
     * 查询是否处于聚焦状态。
     * 方法名避免与 JComponent 的 isFocused() 冲突。
     *
     * @return true 表示聚焦中
     */
    protected boolean isFocusedFlag() {
        return focused;
    }

    /**
     * 查询是否处于选中状态。
     *
     * @return true 表示选中
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * 设置选中状态。
     * <p>
     * 状态变化时触发选中动画和 ItemListener 通知。
     *
     * @param selected true 选中，false 取消选中
     */
    public void setSelected(boolean selected) {
        if (this.selected == selected) return;
        boolean old = this.selected;
        this.selected = selected;
        anim.go(AnimationManager.SELECTED, old ? 1f : 0f, selected ? 1f : 0f);
        fireItemStateChanged(selected);
        onSelectedChanged(selected);
    }

    // ==================== 状态变更钩子 ====================

    /**
     * 悬停状态变更钩子，子类可重写。
     *
     * @param hovering true 表示进入悬停，false 表示离开悬停
     */
    protected void onHoverChanged(boolean hovering) {}

    /**
     * 激活状态变更钩子，子类可重写。
     *
     * @param active true 表示进入激活（按下），false 表示退出激活
     */
    protected void onActiveChanged(boolean active) {}

    /**
     * 聚焦状态变更钩子，子类可重写。
     *
     * @param focused true 表示获得焦点，false 表示失去焦点
     */
    protected void onFocusChanged(boolean focused) {}

    /**
     * 选中状态变更钩子，子类可重写。
     *
     * @param selected true 表示选中，false 表示取消选中
     */
    protected void onSelectedChanged(boolean selected) {}

    /**
     * 是否为切换模式。
     * <p>
     * 切换模式下，鼠标点击会自动切换选中状态（如 Switch、Checkbox、Radio）。
     * 非切换模式下，点击仅触发动作事件，不改变选中状态（如 Button）。
     *
     * @return true 表示切换模式，false 表示动作模式
     */
    protected boolean isToggleMode() {
        return true;
    }

    /**
     * 动作触发钩子，子类可重写。
     * <p>
     * 每次鼠标按下并释放（完成一次点击）时调用，无论是否为切换模式。
     */
    protected void onActionPerformed() {}

    // ==================== 选中状态监听器 ====================

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getSelectedObjects() {
        if (selected) {
            return new Object[] { this };
        }
        return null;
    }

    /**
     * 添加选中状态变更监听器。
     *
     * @param l 监听器
     */
    @Override
    public void addItemListener(ItemListener l) {
        itemListenerList.add(ItemListener.class, l);
    }

    /**
     * 移除选中状态变更监听器。
     *
     * @param l 监听器
     */
    @Override
    public void removeItemListener(ItemListener l) {
        itemListenerList.remove(ItemListener.class, l);
    }

    /**
     * 触发选中状态变更事件。
     *
     * @param selected 当前选中状态
     */
    protected void fireItemStateChanged(boolean selected) {
        ItemEvent e = new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                selected ? ItemEvent.SELECTED : ItemEvent.DESELECTED);
        for (ItemListener l : itemListenerList.getListeners(ItemListener.class)) {
            l.itemStateChanged(e);
        }
    }

    // ==================== 禁用态处理 ====================

    /**
     * 设置组件启用状态。
     * <p>
     * 禁用时重置所有交互状态并停止对应动画，确保组件回到初始视觉状态。
     *
     * @param enabled true 启用，false 禁用
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            hovering = false;
            pressing = false;
            focused = false;
            pressStarted = false;
            anim.stop(AnimationManager.HOVER);
            anim.stop(AnimationManager.ACTIVE);
            anim.stop(AnimationManager.FOCUS);
            anim.stop(AnimationManager.SELECTED);
        }
    }

}

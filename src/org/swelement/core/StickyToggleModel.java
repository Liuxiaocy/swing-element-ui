package org.swelement.core;

import javax.swing.JToggleButton;

/**
 * 选择控件的按钮模型：按下期间拒绝解除武装。
 *
 * <p>Swing 默认行为下，{@code BasicButtonListener.mouseExited} 会调用
 * {@code model.setArmed(false)}；随后释放时 {@code ToggleButtonModel.setPressed(false)}
 * 因 {@code isArmed()} 为假而跳过状态翻转。用户在多个选择控件之间快速轮流点击时，
 * 指针往往在按下与释放之间就已离开控件边界，于是这一次点击被静默丢弃。
 *
 * <p>本模型在 {@code isPressed()} 期间忽略解除武装请求，使「按下 → 移出 → 释放」
 * 仍能完成翻转。代价是按下后拖离控件再释放不再能取消本次操作——对勾选框、
 * 单选框、开关这类低风险的状态切换而言，这个取舍是值得的。
 *
 * <p>释放时序仍然安全：{@code mouseReleased} 先调 {@code setPressed(false)}
 * 完成翻转，再调 {@code setArmed(false)}；此时 {@code isPressed()} 已为假，
 * 解除武装正常生效，不会残留武装态。
 */
public class StickyToggleModel extends JToggleButton.ToggleButtonModel {

    @Override
    public void setArmed(boolean armed) {
        if (!armed && isPressed()) return;
        super.setArmed(armed);
    }
}

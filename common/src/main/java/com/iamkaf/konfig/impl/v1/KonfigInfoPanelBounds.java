//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

final class KonfigInfoPanelBounds {
    final int left;
    final int top;
    final int right;
    final int bottom;
    final int bridgeLeft;
    final int bridgeTop;
    final int bridgeRight;
    final int bridgeBottom;

    KonfigInfoPanelBounds(
            int left,
            int top,
            int right,
            int bottom,
            int bridgeLeft,
            int bridgeTop,
            int bridgeRight,
            int bridgeBottom
    ) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.bridgeLeft = bridgeLeft;
        this.bridgeTop = bridgeTop;
        this.bridgeRight = bridgeRight;
        this.bridgeBottom = bridgeBottom;
    }

    boolean containsPanel(double mouseX, double mouseY) {
        return mouseX >= this.left
                && mouseX <= this.right
                && mouseY >= this.top
                && mouseY <= this.bottom;
    }

    boolean containsBridge(double mouseX, double mouseY) {
        return mouseX >= this.bridgeLeft
                && mouseX <= this.bridgeRight
                && mouseY >= this.bridgeTop
                && mouseY <= this.bridgeBottom;
    }
}
//?}

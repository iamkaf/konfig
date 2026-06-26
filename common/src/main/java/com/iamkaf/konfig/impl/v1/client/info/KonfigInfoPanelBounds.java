//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.info;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class KonfigInfoPanelBounds {
    public final int left;
    public final int top;
    public final int right;
    public final int bottom;
    public final int bridgeLeft;
    public final int bridgeTop;
    public final int bridgeRight;
    public final int bridgeBottom;

    public KonfigInfoPanelBounds(
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

    public boolean containsPanel(double mouseX, double mouseY) {
        return mouseX >= this.left
                && mouseX <= this.right
                && mouseY >= this.top
                && mouseY <= this.bottom;
    }

    public boolean containsBridge(double mouseX, double mouseY) {
        return mouseX >= this.bridgeLeft
                && mouseX <= this.bridgeRight
                && mouseY >= this.bridgeTop
                && mouseY <= this.bridgeBottom;
    }
}
//?}

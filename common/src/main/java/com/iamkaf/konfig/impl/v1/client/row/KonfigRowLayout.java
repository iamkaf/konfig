package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class KonfigRowLayout {
    final int x;
    final int y;
    final int width;
    final int height;
    final int controlWidth;
    final int controlX;
    final int controlY;

    KonfigRowLayout(int x, int y, int width, int height, int controlWidth, int controlX, int controlY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.controlWidth = controlWidth;
        this.controlX = controlX;
        this.controlY = controlY;
    }
}

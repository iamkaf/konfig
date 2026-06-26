//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

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
//?}

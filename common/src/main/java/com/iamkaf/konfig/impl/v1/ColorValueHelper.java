package com.iamkaf.konfig.impl.v1;

import java.util.Locale;

final class ColorValueHelper {
    private ColorValueHelper() {
    }

    static int requireRgb(int value, String path) {
        if ((value & 0xFF000000) != 0) {
            throw new IllegalArgumentException("RGB color out of range for '" + path + "'.");
        }
        return value;
    }

    static int parseRgb(String raw, String path) {
        String hex = normalizeHex(raw, 6, path);
        return Integer.parseInt(hex, 16);
    }

    static int parseArgb(String raw, String path) {
        String hex = normalizeHex(raw, 8, path);
        int red = Integer.parseInt(hex.substring(0, 2), 16);
        int green = Integer.parseInt(hex.substring(2, 4), 16);
        int blue = Integer.parseInt(hex.substring(4, 6), 16);
        int alpha = Integer.parseInt(hex.substring(6, 8), 16);
        return argb(alpha, red, green, blue);
    }

    static String formatRgb(int rgb) {
        return '#' + String.format(Locale.ROOT, "%06X", Integer.valueOf(rgb & 0xFFFFFF));
    }

    static String formatArgb(int argb) {
        return String.format(
                Locale.ROOT,
                "#%02X%02X%02X%02X",
                Integer.valueOf(red(argb)),
                Integer.valueOf(green(argb)),
                Integer.valueOf(blue(argb)),
                Integer.valueOf(alpha(argb))
        );
    }

    static int red(int color) {
        return (color >>> 16) & 0xFF;
    }

    static int green(int color) {
        return (color >>> 8) & 0xFF;
    }

    static int blue(int color) {
        return color & 0xFF;
    }

    static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    static int rgb(int red, int green, int blue) {
        return ((clampByte(red) & 0xFF) << 16)
                | ((clampByte(green) & 0xFF) << 8)
                | (clampByte(blue) & 0xFF);
    }

    static int argb(int alpha, int red, int green, int blue) {
        return ((clampByte(alpha) & 0xFF) << 24)
                | ((clampByte(red) & 0xFF) << 16)
                | ((clampByte(green) & 0xFF) << 8)
                | (clampByte(blue) & 0xFF);
    }

    static int toRenderColor(EntryKind kind, int value) {
        if (kind == EntryKind.COLOR_RGB) {
            return 0xFF000000 | (value & 0xFFFFFF);
        }
        return value;
    }

    static int expectedDigits(EntryKind kind) {
        return kind == EntryKind.COLOR_ARGB ? 8 : 6;
    }

    private static String normalizeHex(String raw, int expectedDigits, String path) {
        if (raw == null) {
            throw invalidColor(path, expectedDigits);
        }

        String normalized = raw.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.regionMatches(true, 0, "0x", 0, 2)) {
            normalized = normalized.substring(2);
        }

        if (normalized.length() != expectedDigits) {
            throw invalidColor(path, expectedDigits);
        }

        for (int i = 0; i < normalized.length(); i++) {
            if (Character.digit(normalized.charAt(i), 16) < 0) {
                throw invalidColor(path, expectedDigits);
            }
        }

        return normalized.toUpperCase(Locale.ROOT);
    }

    private static IllegalArgumentException invalidColor(String path, int digits) {
        return new IllegalArgumentException("Invalid color for '" + path + "' (expected " + digits + " hex digits).");
    }

    private static int clampByte(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 255) {
            return 255;
        }
        return value;
    }
}

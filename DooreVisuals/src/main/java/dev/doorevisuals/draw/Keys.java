package dev.doorevisuals.draw;

import org.lwjgl.glfw.GLFW;

public final class Keys {
    private Keys() {
    }

    public static String name(int code) {
        if (code != -1 && code != 0) {
            return switch (code) {
                case 32 -> "SPACE";
                case 39 -> "'";
                case 44 -> ",";
                case 45 -> "-";
                case 46 -> ".";
                case 47 -> "/";
                case 59 -> ";";
                case 61 -> "=";
                case 91 -> "[";
                case 92 -> "\\";
                case 93 -> "]";
                case 96 -> "GRAVE";
                case 256 -> "ESC";
                case 257 -> "ENTER";
                case 258 -> "TAB";
                case 259 -> "BACK";
                case 260 -> "INS";
                case 261 -> "DEL";
                case 262 -> "\u2192";
                case 263 -> "\u2190";
                case 264 -> "\u2193";
                case 265 -> "\u2191";
                case 266 -> "PGUP";
                case 267 -> "PGDN";
                case 268 -> "HOME";
                case 269 -> "END";
                case 280 -> "CAPS";
                case 281 -> "SCRL";
                case 282 -> "NUM";
                case 283 -> "PRT";
                case 284 -> "PAUSE";
                case 340 -> "LSHIFT";
                case 341 -> "LCTRL";
                case 342 -> "LALT";
                case 343 -> "LWIN";
                case 344 -> "RSHIFT";
                case 345 -> "RCTRL";
                case 346 -> "RALT";
                case 347 -> "RWIN";
                case 348 -> "MENU";
                default -> {
                    String s = GLFW.glfwGetKeyName(code, 0);
                    yield s != null && !s.isBlank()
                        ? s.toUpperCase()
                        : (code >= 290 && code <= 314 ? "F" + (code - 290 + 1) : (code >= 320 && code <= 329 ? "NUM" + (code - 320) : "KEY" + code));
                }
            };
        } else {
            return "\u043d\u0435\u0442";
        }
    }
}

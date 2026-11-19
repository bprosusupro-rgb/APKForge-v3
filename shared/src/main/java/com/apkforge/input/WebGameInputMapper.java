package com.apkforge.input;

/** Maps Android controller buttons to the keyboard keys commonly used by browser games. */
public final class WebGameInputMapper {
    public static final class Key {
        public final String key;
        public final String code;
        public final int keyCode;

        Key(String key, String code, int keyCode) {
            this.key = key;
            this.code = code;
            this.keyCode = keyCode;
        }
    }

    private WebGameInputMapper() { }

    public static Key fromAndroidKeyCode(int keyCode) {
        switch (keyCode) {
            case 19: return new Key("ArrowUp", "ArrowUp", 38);
            case 20: return new Key("ArrowDown", "ArrowDown", 40);
            case 21: return new Key("ArrowLeft", "ArrowLeft", 37);
            case 22: return new Key("ArrowRight", "ArrowRight", 39);
            case 23:
            case 66:
            case 96: return new Key("Enter", "Enter", 13);
            case 97:
            case 111: return new Key("Escape", "Escape", 27);
            case 62:
            case 99: return new Key(" ", "Space", 32);
            case 100: return new Key("Tab", "Tab", 9);
            default: return null;
        }
    }

    /** Maps the built-in touch overlay controls to common browser-game keyboard controls. */
    public static Key fromTouchControlName(String control) {
        if (control == null) return null;
        switch (control) {
            case "up": return new Key("ArrowUp", "ArrowUp", 38);
            case "down": return new Key("ArrowDown", "ArrowDown", 40);
            case "left": return new Key("ArrowLeft", "ArrowLeft", 37);
            case "right": return new Key("ArrowRight", "ArrowRight", 39);
            case "a": case "space": case "jump": return new Key(" ", "Space", 32);
            case "b": case "enter": case "confirm": return new Key("Enter", "Enter", 13);
            case "x": case "escape": case "back": return new Key("Escape", "Escape", 27);
            case "y": case "tab": return new Key("Tab", "Tab", 9);
            default: return null;
        }
    }
}

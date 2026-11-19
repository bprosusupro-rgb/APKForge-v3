package com.gamebridge.mobile;

import com.apkforge.input.WebGameInputMapper;
import java.lang.reflect.Method;

public final class WebGameInputMapperTest {
    private static void key(int androidCode, String expectedKey, String expectedCode, int expectedKeyCode) {
        WebGameInputMapper.Key mapped = WebGameInputMapper.fromAndroidKeyCode(androidCode);
        if (mapped == null || !expectedKey.equals(mapped.key) || !expectedCode.equals(mapped.code)
                || expectedKeyCode != mapped.keyCode) {
            throw new AssertionError("Android key " + androidCode + " was not mapped to " + expectedKey);
        }
    }

    public static void main(String[] args) {
        key(19, "ArrowUp", "ArrowUp", 38);
        key(22, "ArrowRight", "ArrowRight", 39);
        key(96, "Enter", "Enter", 13);
        key(97, "Escape", "Escape", 27);
        key(99, " ", "Space", 32);
        key(100, "Tab", "Tab", 9);
        if (WebGameInputMapper.fromAndroidKeyCode(4) != null) {
            throw new AssertionError("Android Back must remain available to the system instead of becoming a game key");
        }
        touchKey("up", "ArrowUp", "ArrowUp", 38);
        touchKey("left", "ArrowLeft", "ArrowLeft", 37);
        touchKey("a", " ", "Space", 32);
        touchKey("b", "Enter", "Enter", 13);
        touchKey("x", "Escape", "Escape", 27);
        touchKey("unknown", null, null, 0);
        System.out.println("WebGameInputMapper tests passed");
    }

    private static void touchKey(String control, String expectedKey, String expectedCode, int expectedKeyCode) {
        try {
            Method resolver = WebGameInputMapper.class.getMethod("fromTouchControlName", String.class);
            WebGameInputMapper.Key actual = (WebGameInputMapper.Key) resolver.invoke(null, control);
            if (expectedKey == null) {
                if (actual != null) throw new AssertionError("Unknown touch control must be ignored");
            } else if (actual == null || !expectedKey.equals(actual.key) || !expectedCode.equals(actual.code)
                    || expectedKeyCode != actual.keyCode) {
                throw new AssertionError("Touch control '" + control + "' did not map to " + expectedKey);
            }
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("Touch controls need a shared key mapper", missing);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Touch control mapping could not be invoked", failure);
        }
    }
}

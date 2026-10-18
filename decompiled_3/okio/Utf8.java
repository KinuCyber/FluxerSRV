/*
 * Decompiled with CFR 0.152.
 */
package okio;

public final class Utf8 {
    private Utf8() {
    }

    public static long size(String string) {
        return Utf8.size(string, 0, string.length());
    }

    public static long size(String string, int beginIndex, int endIndex) {
        if (string == null) {
            throw new IllegalArgumentException("string == null");
        }
        if (beginIndex < 0) {
            throw new IllegalArgumentException("beginIndex < 0: " + beginIndex);
        }
        if (endIndex < beginIndex) {
            throw new IllegalArgumentException("endIndex < beginIndex: " + endIndex + " < " + beginIndex);
        }
        if (endIndex > string.length()) {
            throw new IllegalArgumentException("endIndex > string.length: " + endIndex + " > " + string.length());
        }
        long result = 0L;
        int i = beginIndex;
        while (i < endIndex) {
            char low;
            char c = string.charAt(i);
            if (c < '\u0080') {
                ++result;
                ++i;
                continue;
            }
            if (c < '\u0800') {
                result += 2L;
                ++i;
                continue;
            }
            if (c < '\ud800' || c > '\udfff') {
                result += 3L;
                ++i;
                continue;
            }
            char c2 = low = i + 1 < endIndex ? string.charAt(i + 1) : (char)'\u0000';
            if (c > '\udbff' || low < '\udc00' || low > '\udfff') {
                ++result;
                ++i;
                continue;
            }
            result += 4L;
            i += 2;
        }
        return result;
    }
}


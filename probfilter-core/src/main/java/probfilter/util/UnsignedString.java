package probfilter.util;


public class UnsignedString {
    private UnsignedString() {}

    public static String from(byte b) {
        return from(Byte.toUnsignedInt(b));
    }

    public static String from(short s) {
        return from(Short.toUnsignedInt(s));
    }

    public static String from(int i) {
        return Integer.toUnsignedString(i);
    }
}

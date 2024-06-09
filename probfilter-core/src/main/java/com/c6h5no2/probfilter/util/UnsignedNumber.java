package com.c6h5no2.probfilter.util;


public final class UnsignedNumber {
    private UnsignedNumber() {}

    public static int compare(byte x, byte y) {
        return Byte.compareUnsigned(x, y);
    }

    public static int compare(byte x, short y) {
        return compare(toUInt(x), toUInt(y));
    }

    public static int compare(byte x, int y) {
        return compare(toUInt(x), y);
    }

    public static int compare(byte x, long y) {
        return compare(toULong(x), y);
    }

    public static int compare(short x, byte y) {
        return compare(toUInt(x), toUInt(y));
    }

    public static int compare(short x, short y) {
        return Short.compareUnsigned(x, y);
    }

    public static int compare(short x, int y) {
        return compare(toUInt(x), y);
    }

    public static int compare(short x, long y) {
        return compare(toULong(x), y);
    }

    public static int compare(int x, byte y) {
        return compare(x, toUInt(y));
    }

    public static int compare(int x, short y) {
        return compare(x, toUInt(y));
    }

    public static int compare(int x, int y) {
        return Integer.compareUnsigned(x, y);
    }

    public static int compare(int x, long y) {
        return compare(toULong(x), y);
    }

    public static int compare(long x, byte y) {
        return compare(x, toULong(y));
    }

    public static int compare(long x, short y) {
        return compare(x, toULong(y));
    }

    public static int compare(long x, int y) {
        return compare(x, toULong(y));
    }

    public static int compare(long x, long y) {
        return Long.compareUnsigned(x, y);
    }

    public static int toUInt(byte b) {
        return Byte.toUnsignedInt(b);
    }

    public static int toUInt(short s) {
        return Short.toUnsignedInt(s);
    }

    public static int toUInt(int i) {
        return i;
    }

    public static long toULong(byte b) {
        return Byte.toUnsignedLong(b);
    }

    public static long toULong(short s) {
        return Short.toUnsignedLong(s);
    }

    public static long toULong(int i) {
        return Integer.toUnsignedLong(i);
    }

    public static long toULong(long l) {
        return l;
    }

    public static String toString(byte b) {
        return toString(toUInt(b));
    }

    public static String toString(short s) {
        return toString(toUInt(s));
    }

    public static String toString(int i) {
        return Integer.toUnsignedString(i);
    }

    public static String toString(long l) {
        return Long.toUnsignedString(l);
    }
}

package eval.util;


public record Slice(int from, int length) {
    public int until() {
        return from + length;
    }

    public static Slice fromLength(int from, int length) {
        return new Slice(from, length);
    }

    public static Slice fromUntil(int from, int until) {
        return new Slice(from, until - from);
    }

    public static Slice fromTo(int from, int to) {
        return new Slice(from, to - from + 1);
    }
}

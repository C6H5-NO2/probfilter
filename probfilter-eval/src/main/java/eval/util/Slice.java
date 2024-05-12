package eval.util;


public record Slice(int from, int length) {
    public int until() {
        return from + length;
    }

    public scala.Tuple2<Slice, Slice> split(double ratio) {
        var slice1 = Slice.fromLength(this.from, (int) (this.length * ratio));
        var slice2 = Slice.fromUntil(slice1.until(), this.until());
        return new scala.Tuple2<>(slice1, slice2);
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

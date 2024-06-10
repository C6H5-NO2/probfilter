package eval.util;

import scala.Tuple2;
import scala.collection.immutable.Range;
import scala.collection.immutable.Range$;


public record Slice(int from, int length) {
    public static Slice fromLength(int from, int length) {
        return new Slice(from, length);
    }

    public static Slice fromUntil(int from, int until) {
        return new Slice(from, until - from);
    }

    public static Slice fromTo(int from, int to) {
        return new Slice(from, to - from + 1);
    }

    public int until() {
        return from + length;
    }

    public Tuple2<Slice, Slice> split(double ratio) {
        var slice1 = Slice.fromLength(this.from, (int) (this.length * ratio));
        var slice2 = Slice.fromUntil(slice1.until(), this.until());
        return new Tuple2<>(slice1, slice2);
    }

    public Range toRange() {
        return Range$.MODULE$.apply(this.from(), this.until());
    }
}

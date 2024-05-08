package eval.util;

import scala.Tuple2;

import java.util.function.Supplier;


public final class Timed {
    private Timed() {}

    public static long measure(Runnable func) {
        long startTime = System.currentTimeMillis();
        func.run();
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    public static <T> Tuple2<T, Long> measure(Supplier<T> func) {
        long startTime = System.currentTimeMillis();
        var result = func.get();
        long endTime = System.currentTimeMillis();
        return new Tuple2<>(result, endTime - startTime);
    }
}

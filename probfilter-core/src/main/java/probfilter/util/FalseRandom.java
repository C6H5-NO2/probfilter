package probfilter.util;


/**
 * It is not pseudo-random; it is false, yet fast.
 */
public final class FalseRandom {
    private static int counter = -1;

    public static int next(int until) {
        var n = (counter + 1) % until;
        counter = n;
        return n;
    }
}

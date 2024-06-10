package eval.data;

import eval.int128.CachedRandomInt128;
import eval.int128.Int128Array;


public final class Dataset {
    // vvv configured vvv
    public static final int SEED = 2024;
    public static final int MAX_MAGNITUDE = 20;
    public static final int MAX_REPEAT = 5;
    // ^^^ configured ^^^

    // vvv deduced vvv
    public static final String DATA_PATHNAME = String.format("random128_%d_%d_%d.bin", SEED, MAX_MAGNITUDE, MAX_REPEAT);
    public static final int START_OF_TESTS = (1 << MAX_MAGNITUDE) * MAX_REPEAT;
    public static final int LENGTH_OF_BATCH = 1 << MAX_MAGNITUDE;
    // ^^^ deduced ^^^

    private static Int128Array data = null;

    private Dataset() {}

    /**
     * @apiNote NOT thread-safe
     */
    public static Int128Array acquire() {
        if (data == null) {
            data = CachedRandomInt128.load(DATA_PATHNAME);
        }
        return data;
    }

    /**
     * @apiNote Release the static reference in this class only. All other references shall also be
     * set to {@code null} to make the data unreachable. Then one can call {@code System.gc()}.
     */
    public static void release() {
        data = null;
    }

    public static void main(String[] args) {
        CachedRandomInt128.dump(SEED, MAX_MAGNITUDE, MAX_REPEAT, DATA_PATHNAME);
    }
}

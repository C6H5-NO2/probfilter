package eval.data;

import eval.int128.CachedRandomInt128;


public final class Dataset {
    private Dataset() {}

    // vvv configured vvv
    private static final int SEED = 2024;
    public static final int MAX_MAGNITUDE = 20;
    public static final int MAX_REPEAT = 5;
    // ^^^ configured ^^^

    // vvv deduced vvv
    public static final String DATA_PATHNAME = String.format("random128_%d_%d_%d.bin", SEED, MAX_MAGNITUDE, MAX_REPEAT);
    public static final int START_OF_TESTS = (1 << MAX_MAGNITUDE) * MAX_REPEAT;
    public static final int LENGTH_OF_BATCH = 1 << MAX_MAGNITUDE;
    // ^^^ deduced ^^^

    public static void main(String[] args) {
        CachedRandomInt128.dump(SEED, MAX_MAGNITUDE, MAX_REPEAT, DATA_PATHNAME);
    }
}

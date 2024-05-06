package eval.int128;

import java.io.File;
import java.io.IOException;


public final class CachedRandomInt128 {
    private CachedRandomInt128() {}

    public static void dump(long seed, int magnitude, int repeat, String pathname) {
        var file = new File(pathname);
        if (file.exists())
            throw new RuntimeException();
        var rnd = new RandomInt128(seed);
        var capacity = (1 << magnitude) * 2 * repeat;
        var array = rnd.distinct(capacity);
        try {
            array.writeFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Int128Array load(String pathname) {
        var file = new File(pathname);
        var array = new Int128Array(0);
        try {
            array.readFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return array;
    }
}

package eval.int128;

import java.util.Random;
import java.util.TreeSet;


public final class RandomInt128Generator {
    private final Random random;

    public RandomInt128Generator(long seed) {
        this.random = new Random(seed);
    }

    public Int128 nextInt128() {
        long high = random.nextLong();
        long low = random.nextLong();
        return new Int128(high, low);
    }

    public Int128Array distinct(int size) {
        var array = new Int128Array(size);
        var set = new TreeSet<Int128>();
        for (int i = 0; i < size; ) {
            var int128 = nextInt128();
            if (set.contains(int128)) {
                continue;
            }
            set.add(int128);
            array.set(i, int128);
            ++i;
        }
        return array;
    }
}

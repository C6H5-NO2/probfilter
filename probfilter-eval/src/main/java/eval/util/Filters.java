package eval.util;

import eval.int128.Int128;
import eval.int128.Int128Array;
import probfilter.pdsa.Filter;
import scala.Tuple2;


public final class Filters {
    private Filters() {}

    /**
     * @return a {@link scala.Tuple2} of the filled filter and number of successful insertions
     */
    public static Tuple2<Filter<Int128>, Integer> fill(Filter<Int128> filter, Int128Array data, Slice src) {
        return fill(filter, data, src, false);
    }

    public static Tuple2<Filter<Int128>, Integer> fill(Filter<Int128> filter, Int128Array data, Slice src, boolean forceAll) {
        int numSuccess = 0;
        for (int i = src.from(); i < src.until(); ++i) {
            var int128 = data.get(i);
            var result = filter.tryAdd(int128);
            if (result.isSuccess()) {
                filter = result.get();
                ++numSuccess;
            } else {
                if (!forceAll)
                    break;
            }
        }
        return new Tuple2<>(filter, numSuccess);
    }

    public static double measureFpp(Filter<Int128> filter, Int128Array data, Slice tests) {
        int numFP = 0;
        for (int i = tests.from(); i < tests.until(); ++i) {
            var int128 = data.get(i);
            boolean result = filter.contains(int128);
            if (result) {
                ++numFP;
            }
        }
        return (double) numFP / tests.length();
    }
}

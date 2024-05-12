package eval.util;

import eval.int128.Int128;
import probfilter.pdsa.Filter;


@FunctionalInterface
public interface FilterSupplier {
    Filter<Int128> get(int capacity, short rid);
}

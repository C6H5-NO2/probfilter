package eval.util;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.int128.Int128;


@FunctionalInterface
public interface FilterSupplier {
    FluentCvRFilter<Int128> get(int capacity, short rid);
}

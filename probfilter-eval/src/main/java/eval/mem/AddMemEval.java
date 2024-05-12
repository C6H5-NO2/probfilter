package eval.mem;

import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.FilterSupplier;
import eval.util.Filters;
import eval.util.Slice;
import probfilter.pdsa.Filter;

import java.io.BufferedWriter;


public final class AddMemEval extends AbstractMemEval {
    private final FilterSupplier supplier;

    public AddMemEval(int loadMagnitude, int repeat, FilterSupplier supplier) {
        super(loadMagnitude, repeat);
        this.supplier = supplier;
    }

    @Override
    protected Filter<Int128> loadFilter(int capacity, Int128Array data, int load, int epoch) {
        var addSlice = Slice.fromLength(load * epoch, load);
        var filter = supplier.get(capacity, (short) 1);
        filter = Filters.fill(filter, data, addSlice).filter();
        return filter;
    }

    @Override
    protected void preLoadStep(BufferedWriter writer, int load) {
        int magnitude = Integer.numberOfTrailingZeros(load);
        System.out.printf("load: local 1.00-add %d (2^%d)%n", load, magnitude);
    }
}

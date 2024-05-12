package eval.mem;

import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.FilterSupplier;
import eval.util.Filters;
import eval.util.Slice;
import probfilter.pdsa.Filter;

import java.io.BufferedWriter;


public final class Dist2AddMemEval extends AbstractMemEval {
    private final double splitRatio;
    private final FilterSupplier supplier;

    public Dist2AddMemEval(int loadMagnitude, int repeat, double splitRatio, FilterSupplier supplier) {
        super(loadMagnitude, repeat);
        if (splitRatio < .5 || splitRatio > 1)
            throw new IllegalArgumentException();
        this.splitRatio = splitRatio;
        this.supplier = supplier;
    }

    @Override
    protected Filter<Int128> loadFilter(int capacity, Int128Array data, int load, int epoch) {
        var tuple = Slice.fromLength(load * epoch, load).split(splitRatio);
        var addSlice1 = tuple._1;
        var addSlice2 = tuple._2;
        var filter1 = supplier.get(capacity, (short) 1);
        var filter2 = supplier.get(capacity, (short) 2);
        filter1 = Filters.fill(filter1, data, addSlice1).filter();
        filter2 = Filters.fill(filter2, data, addSlice2).filter();
        return Filters.merge(filter1, filter2);
    }

    @Override
    protected void preLoadStep(BufferedWriter writer, int load) {
        int magnitude = Integer.numberOfTrailingZeros(load);
        System.out.printf("load: %.2f-dist-2 1.00-add %d (2^%d)%n", splitRatio, load, magnitude);
    }
}

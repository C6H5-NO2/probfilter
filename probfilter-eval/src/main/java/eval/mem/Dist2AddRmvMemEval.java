package eval.mem;

import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.FilterSupplier;
import eval.util.Filters;
import eval.util.Slice;
import probfilter.pdsa.Filter;

import java.io.BufferedWriter;


public final class Dist2AddRmvMemEval extends AbstractMemEval {
    private final double addRatio;
    private final double splitRatio;
    private final FilterSupplier supplier;

    public Dist2AddRmvMemEval(int loadMagnitude, int repeat, double addRatio, double splitRatio, FilterSupplier supplier) {
        super(loadMagnitude, repeat);
        this.addRatio = addRatio;
        this.splitRatio = splitRatio;
        this.supplier = supplier;
    }

    @Override
    protected Filter<Int128> loadFilter(int capacity, Int128Array data, int load, int epoch) {
        // firstly add-rmv-split, then split to dist-2
        var loadSlice = Slice.fromLength(load * epoch, load);
        var tuple = loadSlice.split(addRatio);
        var addSlice = tuple._1;
        var rmvSlice = tuple._2;
        tuple = addSlice.split(splitRatio);
        var addSlice1 = tuple._1;
        var addSlice2 = tuple._2;
        tuple = rmvSlice.split(splitRatio);
        var rmvSlice1 = tuple._1;
        var rmvSlice2 = tuple._2;

        var filter1 = supplier.get(capacity, (short) 1);
        var filter2 = supplier.get(capacity, (short) 2);
        filter1 = Filters.fill(filter1, data, addSlice1).filter();
        filter2 = Filters.fill(filter2, data, addSlice2).filter();
        var insSlice1 = Slice.fromLength(addSlice1.from(), filter1.size());
        var insSlice2 = Slice.fromLength(addSlice2.from(), filter2.size());

        filter1 = Filters.merge(filter1, filter2);
        // should yield an equivalent copy, assuming merge is correctly implemented
        filter2 = Filters.merge(filter2, filter1);

        filter1 = Filters.drop(filter1, data, rmvSlice1, insSlice1, insSlice2);
        filter2 = Filters.drop(filter2, data, rmvSlice2, insSlice1, insSlice2);

        return Filters.merge(filter1, filter2);
    }

    @Override
    protected void preLoadStep(BufferedWriter writer, int load) {
        int magnitude = Integer.numberOfTrailingZeros(load);
        System.out.printf("load: %.2f-dist-2 %.2f-add-rmv %d (2^%d)%n", splitRatio, addRatio, load, magnitude);
    }
}

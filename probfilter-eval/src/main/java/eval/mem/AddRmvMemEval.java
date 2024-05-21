package eval.mem;

import eval.data.Dataset;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.FilterSupplier;
import eval.util.Filters;
import eval.util.Slice;
import probfilter.pdsa.Filter;

import java.io.BufferedWriter;
import java.util.Random;


final class AddRmvMemEval extends AbstractMemEval {
    private final double addRatio;
    private final FilterSupplier supplier;
    private final Random rnd;

    AddRmvMemEval(int loadMagnitude, int repeat, double addRatio, FilterSupplier supplier) {
        super(loadMagnitude, repeat);
        if (addRatio < .5 || addRatio > 1)
            throw new IllegalArgumentException();
        this.addRatio = addRatio;
        this.supplier = supplier;
        this.rnd = new Random(Dataset.SEED + 1);
    }

    @Override
    protected Filter<Int128> loadFilter(int capacity, Int128Array data, int load, int epoch) {
        var tuple = Slice.fromLength(load * epoch, load).split(addRatio);
        var addSlice = tuple._1;
        var rmvSlice = tuple._2;
        var filter = supplier.get(load, (short) 1);
        filter = Filters.fill(filter, data, addSlice).filter();
        var insSlice = Slice.fromLength(addSlice.from(), filter.size());
        filter = Filters.drop(filter, data, rnd, rmvSlice.length(), insSlice);
        return filter;
    }

    @Override
    protected void preLoadStep(BufferedWriter writer, int load) {
        int magnitude = Integer.numberOfTrailingZeros(load);
        System.out.printf("load: local %.2f-add-rmv %d (2^%d)%n", addRatio, load, magnitude);
    }
}

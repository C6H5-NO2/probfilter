package eval.mem;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.filter.FilterSupplier;
import eval.filter.Filters;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.Slice;

import java.io.IOException;
import java.util.Random;


public final class LocalMemEvalLoop extends MemEvalLoop {
    private final int loadMagnitude;
    private final double addRatio;
    private final FilterSupplier supplier;

    public LocalMemEvalLoop(int loadMagnitude, int repeat, double addRatio, FilterSupplier supplier) {
        super(loadMagnitude, repeat);
        this.loadMagnitude = loadMagnitude;
        this.addRatio = addRatio;
        this.supplier = supplier;
        if (addRatio < 0.5 || addRatio > 1)
            throw new IllegalArgumentException();
    }

    @Override
    protected void preVarLoop() throws IOException {
        System.out.printf("load: local %d (2^%d) %.2f-add", 1 << loadMagnitude, loadMagnitude, addRatio);
        if (isAddOnly()) {
            System.out.print(" [+]");
        }
        System.out.println();
        super.preVarLoop();
    }

    @Override
    protected void preVarStep(int variable) {
        System.out.printf("epoch %d%n", variable);
    }

    @Override
    protected FluentCvRFilter<Int128> loadFilter(int capacity, Int128Array data, int load, int epoch) {
        var slices = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load).split(addRatio);
        var sliceToAdd = slices._1;
        var sizeToRmv = slices._2.length();
        var filter = supplier.get(capacity, (short) 1);
        var fillResult = Filters.fill(filter, data, sliceToAdd);
        filter = fillResult._1;
        if (isAddOnly()) {
            return filter;
        } else {
            var rng = new Random(Dataset.SEED + 1 + epoch);
            var sliceInserted = Slice.fromLength(sliceToAdd.from(), fillResult._2);
            return Filters.drop(filter, data, sizeToRmv, rng, sliceInserted);
        }
    }

    private boolean isAddOnly() {
        return addRatio > 0.999;
    }
}

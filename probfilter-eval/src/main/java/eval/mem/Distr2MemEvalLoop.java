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


public final class Distr2MemEvalLoop extends MemEvalLoop {
    private final int loadMagnitude;
    private final double addRatio;
    private final double distrRatio;
    private final FilterSupplier supplier;

    public Distr2MemEvalLoop(int loadMagnitude, int repeat, double addRatio, double distrRatio, FilterSupplier supplier) {
        super(loadMagnitude, repeat);
        this.loadMagnitude = loadMagnitude;
        this.addRatio = addRatio;
        this.distrRatio = distrRatio;
        this.supplier = supplier;
        if (addRatio < 0.5 || addRatio > 1)
            throw new IllegalArgumentException();
    }

    @Override
    protected void preVarLoop() throws IOException {
        System.out.printf(
            "load: distr2 %d (2^%d) %.2f-add %.2f-split",
            1 << loadMagnitude,
            loadMagnitude,
            addRatio,
            distrRatio
        );
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
        // firstly split to add-rmv, then split to distr-2
        var slices = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load).split(addRatio);
        var slicesToAdd = slices._1;
        var slicesToRmv = slices._2;
        var sliceToAdd1 = slicesToAdd.split(distrRatio)._1;
        var sliceToAdd2 = slicesToAdd.split(distrRatio)._2;

        var filter1 = supplier.get(capacity, (short) 1);
        var filter2 = supplier.get(capacity, (short) 2);
        var fillResult1 = Filters.fill(filter1, data, sliceToAdd1);
        var fillResult2 = Filters.fill(filter2, data, sliceToAdd2);
        filter1 = fillResult1._1;
        filter2 = fillResult2._1;

        if (isAddOnly()) {
            return filter1.merge(filter2);
        } else {
            // it works given merge is correctly implemented to yield LUB
            filter1 = filter1.merge(filter2);
            filter2 = filter2.merge(filter1);
            var sizeToRmv1 = slicesToRmv.split(distrRatio)._1.length();
            var sizeToRmv2 = slicesToRmv.split(distrRatio)._2.length();
            var rng1 = new Random(Dataset.SEED + 1 + epoch);
            var rng2 = new Random(Dataset.SEED - 1 - epoch);
            var sliceInserted1 = Slice.fromLength(sliceToAdd1.from(), fillResult1._2);
            var sliceInserted2 = Slice.fromLength(sliceToAdd2.from(), fillResult2._2);
            filter1 = Filters.drop(filter1, data, sizeToRmv1, rng1, sliceInserted1);
            filter2 = Filters.drop(filter2, data, sizeToRmv2, rng2, sliceInserted2);
            return filter1.merge(filter2);
        }
    }

    private boolean isAddOnly() {
        return addRatio > 0.999;
    }
}

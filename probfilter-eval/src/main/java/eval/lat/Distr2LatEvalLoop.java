package eval.lat;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.filter.FilterSupplier;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.Slice;

import java.util.Random;


public final class Distr2LatEvalLoop extends LatEvalLoop {
    private final double distrRatio = 0.50;

    private final int loadMagnitude;
    private final double addRatio;
    private final FilterSupplier supplier;

    public Distr2LatEvalLoop(
        int loadMagnitude,
        int repeat,
        Slice syncFreqMagnitudeRange,
        double addRatio,
        FilterSupplier supplier
    ) {
        super(loadMagnitude, repeat, syncFreqMagnitudeRange);
        this.loadMagnitude = loadMagnitude;
        this.addRatio = addRatio;
        this.supplier = supplier;
        if (addRatio < 0.5 || addRatio > 1)
            throw new IllegalArgumentException();
    }

    @Override
    protected void preVarStep(int variable) {
        System.out.printf(
            "load: distr2 %d (2^%d) %.2f-add %.2f-split sync per %d",
            1 << loadMagnitude,
            loadMagnitude,
            addRatio,
            distrRatio,
            (int) Math.pow(10, variable)
        );
        if (isAddOnly()) {
            System.out.print(" [+]");
        }
        System.out.println();
    }

    @Override
    protected FluentCvRFilter<Int128>
    loadFilter(int capacity, Int128Array data, int load, int epoch, int syncFreq) {
        var loadSlice = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load);
        var slicesToAdd = loadSlice.split(addRatio)._1;
        var sliceToAdd1 = slicesToAdd.split(distrRatio)._1;
        var sliceToAdd2 = slicesToAdd.split(distrRatio)._2;

        var empty1 = supplier.get(capacity, (short) 1);
        var empty2 = supplier.get(capacity, (short) 2);
        var rng1 = new Random(Dataset.SEED + 1 + epoch);
        var rng2 = new Random(Dataset.SEED - 1 - epoch);
        var filter1 = new TrackedFilter(empty1, data, sliceToAdd1, addRatio, rng1, slicesToAdd);
        var filter2 = new TrackedFilter(empty2, data, sliceToAdd2, addRatio, rng2, slicesToAdd);

        var rng = new Random(epoch);
        for (int i = 0; i < load; ++i) {
            if (isAddOnly() && filter1.isFull() && filter2.isFull()) {
                break;
            }
            if (rng.nextDouble() < distrRatio) {
                if (isAddOnly() && filter1.isFull()) {
                    continue;
                }
                filter1.step();
                if (filter1.getNumAppliedOps() % syncFreq == 0) {
                    filter1.merge(filter2);
                }
            } else {
                if (isAddOnly() && filter2.isFull()) {
                    continue;
                }
                filter2.step();
                if (filter2.getNumAppliedOps() % syncFreq == 0) {
                    filter2.merge(filter1);
                }
            }
        }

        if (!isAddOnly()) {
            System.out.printf(
                "#fn: (%d+%d)/%d%n",
                filter1.getNumFalseNegatives(),
                filter2.getNumFalseNegatives(),
                loadSlice.split(addRatio)._2.length()
            );
        }

        return filter1.merge(filter2).getFilter();
    }

    private boolean isAddOnly() {
        return addRatio > 0.999;
    }
}

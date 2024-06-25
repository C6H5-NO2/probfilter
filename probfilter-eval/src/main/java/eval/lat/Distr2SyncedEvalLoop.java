package eval.lat;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.filter.FilterSupplier;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.Slice;

import java.util.Random;


public final class Distr2SyncedEvalLoop extends LatEvalLoop {
    private final double distrRatio = 0.50;

    private final int loadMagnitude;
    private final double addRatio;
    private final FilterSupplier supplier;

    public Distr2SyncedEvalLoop(
        int loadMagnitude,
        int repeat,
        double addRatio,
        FilterSupplier supplier
    ) {
        super(loadMagnitude, repeat, Slice.fromTo(0, 0));
        this.loadMagnitude = loadMagnitude;
        this.addRatio = addRatio;
        this.supplier = supplier;
        if (addRatio < 0.999)
            throw new IllegalArgumentException();
    }

    @Override
    protected void preVarStep(int variable) {
        System.out.printf(
            "load: distr2 %d (2^%d) %.2f-add %.2f-split synced [+]%n",
            1 << loadMagnitude,
            loadMagnitude,
            addRatio,
            distrRatio
        );
    }

    @Override
    protected FluentCvRFilter<Int128>
    loadFilter(int capacity, Int128Array data, int load, int epoch, int syncFreq) {
        var loadSlice = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load);
        var slicesToAdd = loadSlice.split(addRatio)._1;
        var sliceToAdd1 = slicesToAdd.split(distrRatio)._1;
        var sliceToAdd2 = slicesToAdd.split(distrRatio)._2;

        var filter = supplier.get(capacity, (short) 0);
        int indexToAdd1 = sliceToAdd1.from();
        int indexToAdd2 = sliceToAdd2.from();

        var rng = new Random(epoch);
        for (int i = 0; i < load; ++i) {
            if (indexToAdd1 >= sliceToAdd1.until() && indexToAdd2 >= sliceToAdd2.until()) {
                break;
            }
            if (rng.nextDouble() < distrRatio) {
                if (indexToAdd1 >= sliceToAdd1.until()) {
                    continue;
                }
                var result = filter.tryAdd(data.get(indexToAdd1));
                if (result.isSuccess()) {
                    filter = result.get();
                    indexToAdd1 += 1;
                } else {
                    indexToAdd1 = sliceToAdd1.until();
                }
            } else {
                if (indexToAdd2 >= sliceToAdd2.until()) {
                    continue;
                }
                var result = filter.tryAdd(data.get(indexToAdd2));
                if (result.isSuccess()) {
                    filter = result.get();
                    indexToAdd2 += 1;
                } else {
                    indexToAdd2 = sliceToAdd2.until();
                }
            }
        }

        return filter;
    }
}

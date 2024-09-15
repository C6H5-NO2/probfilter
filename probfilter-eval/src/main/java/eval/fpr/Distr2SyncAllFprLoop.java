package eval.fpr;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.filter.FilterSupplier;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.Slice;

import java.util.Random;


public final class Distr2SyncAllFprLoop extends FprEvalLoop {
    private final int loadMagnitude;
    private final double distrRatio;
    private final FilterSupplier supplier;

    public Distr2SyncAllFprLoop(
        int loadMagnitude,
        int repeat,
        double distrRatio,
        FilterSupplier supplier
    ) {
        super(loadMagnitude, repeat, Slice.fromTo(0, 0));
        this.loadMagnitude = loadMagnitude;
        this.distrRatio = distrRatio;
        this.supplier = supplier;
        if (distrRatio < 0.5)
            throw new IllegalArgumentException();
    }

    @Override
    protected void preVarStep(int variable) {
        System.out.printf(
            "load: distr2 %d (2^%d) %.2f-add %.2f-split all-sync%n",
            1 << loadMagnitude,
            loadMagnitude,
            1.0,
            distrRatio
        );
    }

    // @Override
    // protected FluentCvRFilter<Int128>
    // loadFilter(int capacity, Int128Array data, int load, int epoch, int syncFreq) {
    //     var slicesToAdd = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load).split(distrRatio);
    //
    //     var empty = supplier.get(capacity, (short) 0);
    //
    //     var filter1 = new TrackedFilter(
    //         empty,
    //         data,
    //         slicesToAdd._1
    //     );
    //     var filter2 = new TrackedFilter(
    //         empty,
    //         data,
    //         slicesToAdd._2
    //     );
    //
    //     var rng = new Random(epoch);
    //     for (int opIndex = 0; opIndex < load; ++opIndex) {
    //         if (!filter1.canAdd() && !filter2.canAdd()) {
    //             break;
    //         }
    //         boolean distrToFilter1 = rng.nextDouble() < distrRatio;
    //         boolean opOnFilter1 =
    //             (distrToFilter1 && filter1.canAdd()) || (!distrToFilter1 && !filter2.canAdd());
    //         if (opOnFilter1) {
    //             filter1.add();
    //             filter2.syncFilter(filter1);
    //         } else {
    //             filter2.add();
    //             filter1.syncFilter(filter2);
    //         }
    //     }
    //
    //     return filter1.getFilter();
    // }

    @Override
    protected FluentCvRFilter<Int128>
    loadFilter(int capacity, Int128Array data, int load, int epoch, int syncFreq) {
        var sliceToAdd = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load);

        var filter = supplier.get(capacity, (short) 0);
        boolean isFull1 = false, isFull2 = false;

        var rng = new Random(epoch);
        for (int opIndex = 0; opIndex < load; ++opIndex) {
            if (isFull1 && isFull2) {
                break;
            }
            int indexToAdd = sliceToAdd.from() + opIndex;
            var elem = data.get(indexToAdd);
            boolean distrToFilter1 = rng.nextDouble() < distrRatio;
            if (distrToFilter1 && !isFull1) {
                var result = filter.tryAdd(elem);
                if (result.isSuccess()) {
                    filter = result.get();
                } else {
                    isFull1 = true;
                }
            } else if (!distrToFilter1 && !isFull2) {
                var result = filter.tryAdd(elem);
                if (result.isSuccess()) {
                    filter = result.get();
                } else {
                    isFull2 = true;
                }
            }
        }

        return filter;
    }
}

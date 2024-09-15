package eval.fpr;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.filter.FilterSupplier;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.Slice;

import java.util.Random;


public final class Distr2SyncPerFprLoop extends FprEvalLoop {
    private final int loadMagnitude;
    private final double distrRatio;
    private final FilterSupplier supplier;

    public Distr2SyncPerFprLoop(
        int loadMagnitude,
        int repeat,
        Slice syncFreqMagnitudeRange,
        double distrRatio,
        FilterSupplier supplier
    ) {
        super(loadMagnitude, repeat, syncFreqMagnitudeRange);
        this.loadMagnitude = loadMagnitude;
        this.distrRatio = distrRatio;
        this.supplier = supplier;
        if (distrRatio < 0.5)
            throw new IllegalArgumentException();
    }

    @Override
    protected void preVarStep(int variable) {
        System.out.printf(
            "load: distr2 %d (2^%d) %.2f-add %.2f-split (1e-%d)-sync%n",
            1 << loadMagnitude,
            loadMagnitude,
            1.0,
            distrRatio,
            variable
        );
    }

    // @Override
    // protected FluentCvRFilter<Int128>
    // loadFilter(int capacity, Int128Array data, int load, int epoch, int syncFreq) {
    //     var slicesToAdd = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load).split(distrRatio);
    //
    //     var filter1 = new TrackedFilter(
    //         supplier.get(capacity, (short) 1),
    //         data,
    //         slicesToAdd._1
    //     );
    //     var filter2 = new TrackedFilter(
    //         supplier.get(capacity, (short) 2),
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
    //             if (filter1.getNumAppliedOps() % syncFreq == 0) {
    //                 filter2.merge(filter1);
    //             }
    //         } else {
    //             filter2.add();
    //             if (filter2.getNumAppliedOps() % syncFreq == 0) {
    //                 filter1.merge(filter2);
    //             }
    //         }
    //     }
    //
    //     filter1.merge(filter2);
    //     return filter1.getFilter();
    // }

    @Override
    protected FluentCvRFilter<Int128>
    loadFilter(int capacity, Int128Array data, int load, int epoch, int syncFreq) {
        var sliceToAdd = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load);

        var filter1 = supplier.get(capacity, (short) 1);
        var filter2 = supplier.get(capacity, (short) 2);
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
                var result = filter1.tryAdd(elem);
                if (result.isSuccess()) {
                    filter1 = result.get();
                } else {
                    isFull1 = true;
                }
            } else if (!distrToFilter1 && !isFull2) {
                var result = filter2.tryAdd(elem);
                if (result.isSuccess()) {
                    filter2 = result.get();
                } else {
                    isFull2 = true;
                }
            }

            if ((opIndex + 1) % syncFreq == 0) {
                var filter1m2 = filter1.merge(filter2);
                var filter2m1 = filter2.merge(filter1);
                filter1 = filter1m2;
                filter2 = filter2m1;
            }
        }

        return filter1.merge(filter2);
    }
}

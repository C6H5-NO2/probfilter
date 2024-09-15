package eval.fpr;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.filter.FilterSupplier;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.Slice;


public final class LocalSyncAllFprLoop extends FprEvalLoop {
    private final int loadMagnitude;
    private final FilterSupplier supplier;

    public LocalSyncAllFprLoop(
        int loadMagnitude,
        int repeat,
        FilterSupplier supplier
    ) {
        super(loadMagnitude, repeat, Slice.fromTo(0, 0));
        this.loadMagnitude = loadMagnitude;
        this.supplier = supplier;
    }

    @Override
    protected void preVarStep(int variable) {
        System.out.printf(
            "load: local %d (2^%d) %.2f-add %.2f-split all-sync%n",
            1 << loadMagnitude,
            loadMagnitude,
            1.0,
            1.0
        );
    }

    @Override
    protected FluentCvRFilter<Int128>
    loadFilter(int capacity, Int128Array data, int load, int epoch, int syncFreq) {
        var sliceToAdd = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load);
        var filter = supplier.get(capacity, (short) 0);
        for (int indexToAdd = sliceToAdd.from(); indexToAdd < sliceToAdd.until(); ++indexToAdd) {
            var elem = data.get(indexToAdd);
            var result = filter.tryAdd(elem);
            if (result.isSuccess()) {
                filter = result.get();
            } else {
                break;
            }
        }
        return filter;
    }
}

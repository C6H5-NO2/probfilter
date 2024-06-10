package eval.fpp;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.filter.FilterSupplier;
import eval.filter.Filters;
import eval.util.Slice;
import scala.Tuple2;


public final class LocalFppEvalLoop extends FppEvalLoop {
    private final FilterSupplier supplier;

    public LocalFppEvalLoop(Slice loadMagnitudeRange, int repeat, FilterSupplier supplier) {
        super(loadMagnitudeRange, repeat);
        this.supplier = supplier;
    }

    @Override
    protected void preVarStep(int variable) {
        System.out.printf("load: local %d (2^%d)%n", 1 << variable, variable);
    }

    @Override
    protected Tuple2<FluentCvRFilter<Int128>, Integer>
    loadFilter(int capacity, Int128Array data, int load, int epoch) {
        var sliceToAdd = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load);
        var filter = supplier.get(capacity, (short) 1);
        return Filters.fill(filter, data, sliceToAdd);
    }
}

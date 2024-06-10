package eval.fpp;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.filter.FilterSupplier;
import eval.filter.Filters;
import eval.util.Slice;
import scala.Tuple2;


public final class Distr2FppEvalLoop extends FppEvalLoop {
    private final double splitRatio;
    private final FilterSupplier supplier;

    public Distr2FppEvalLoop(Slice loadMagnitudeRange, int repeat, double splitRatio, FilterSupplier supplier) {
        super(loadMagnitudeRange, repeat);
        this.splitRatio = splitRatio;
        this.supplier = supplier;
    }

    @Override
    protected void preVarStep(int variable) {
        System.out.printf("load: distr2 %d (2^%d) %.2f-split%n", 1 << variable, variable, splitRatio);
    }

    @Override
    protected Tuple2<FluentCvRFilter<Int128>, Integer>
    loadFilter(int capacity, Int128Array data, int load, int epoch) {
        var sliceToAdd = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load).split(splitRatio);
        var sliceToAdd1 = sliceToAdd._1;
        var sliceToAdd2 = sliceToAdd._2;
        var filter1 = supplier.get(capacity, (short) 1);
        var filter2 = supplier.get(capacity, (short) 2);
        var result1 = Filters.fill(filter1, data, sliceToAdd1);
        var result2 = Filters.fill(filter2, data, sliceToAdd2);
        return new Tuple2<>(result1._1.merge(result2._1), result1._2 + result2._2);
    }
}

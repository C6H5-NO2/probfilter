package eval.fpp;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.filter.FilterSupplier;
import eval.filter.Filters;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.Slice;
import scala.Tuple2;


public final class Distr2FppEvalLoop extends FppEvalLoop {
    private final double distrRatio;
    private final FilterSupplier supplier;

    public Distr2FppEvalLoop(Slice loadMagnitudeRange, int repeat, double distrRatio, FilterSupplier supplier) {
        super(loadMagnitudeRange, repeat);
        this.distrRatio = distrRatio;
        this.supplier = supplier;
    }

    @Override
    protected void preVarStep(int variable) {
        System.out.printf("load: distr2 %d (2^%d) %.2f-split%n", 1 << variable, variable, distrRatio);
    }

    @Override
    protected Tuple2<FluentCvRFilter<Int128>, Integer>
    loadFilter(int capacity, Int128Array data, int load, int epoch) {
        var slicesToAdd = Slice.fromLength(Dataset.LENGTH_OF_BATCH * epoch, load).split(distrRatio);
        var sliceToAdd1 = slicesToAdd._1;
        var sliceToAdd2 = slicesToAdd._2;
        var filter1 = supplier.get(capacity, (short) 1);
        var filter2 = supplier.get(capacity, (short) 2);
        var fillResult1 = Filters.fill(filter1, data, sliceToAdd1);
        var fillResult2 = Filters.fill(filter2, data, sliceToAdd2);
        filter1 = fillResult1._1;
        filter2 = fillResult2._1;
        return new Tuple2<>(filter1.merge(filter2), fillResult1._2 + fillResult2._2);
    }
}

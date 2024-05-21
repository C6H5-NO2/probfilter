package eval.fpp;

import eval.int128.Int128;
import eval.int128.Int128Funnel;
import eval.util.Slice;
import probfilter.crdt.mutable.GBloomFilter;
import probfilter.pdsa.Filter;
import probfilter.pdsa.bloom.SimpleBloomStrategy;


final class FppEvalGBF extends FppEval {
    private FppEvalGBF(Slice loadMagnitude) {
        super(loadMagnitude);
    }

    @Override
    Filter<Int128> supplyFilter(int capacity, short rid) {
        var strategy = SimpleBloomStrategy.create(capacity, 3e-2, new Int128Funnel());
        return new GBloomFilter<>(strategy);
    }

    public static void main(String[] args) {
        var instance = new FppEvalGBF(Slice.fromTo(10, 20));
        instance.evalLocal("results/fpp/gbf_3e-2_1.00.csv");
        instance.evalSplit("results/fpp/gbf_3e-2_0.50.csv", .50);
        instance.evalSplit("results/fpp/gbf_3e-2_0.80.csv", .80);
        instance.evalSplit("results/fpp/gbf_3e-2_0.99.csv", .99);
    }
}

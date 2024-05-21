package eval.fpp;

import eval.int128.Int128;
import eval.int128.Int128Funnel;
import eval.util.Slice;
import probfilter.crdt.immutable.ScGBloomFilter;
import probfilter.pdsa.Filter;
import probfilter.pdsa.bloom.SimpleBloomStrategy;


final class FppEvalScGBF extends FppEval {
    private FppEvalScGBF(Slice loadMagnitude) {
        super(loadMagnitude);
    }

    @Override
    Filter<Int128> supplyFilter(int capacity, short rid) {
        var strategy = SimpleBloomStrategy.create(capacity >> 2, 3e-2, new Int128Funnel());
        return new ScGBloomFilter<>(strategy);
    }

    public static void main(String[] args) {
        var instance = new FppEvalScGBF(Slice.fromTo(10, 20));
        instance.evalLocal("results/fpp/scgbf_3e-2_c4_1.00.csv");
        instance.evalSplit("results/fpp/scgbf_3e-2_c4_0.50.csv", .50);
        instance.evalSplit("results/fpp/scgbf_3e-2_c4_0.80.csv", .80);
        instance.evalSplit("results/fpp/scgbf_3e-2_c4_0.99.csv", .99);
    }
}

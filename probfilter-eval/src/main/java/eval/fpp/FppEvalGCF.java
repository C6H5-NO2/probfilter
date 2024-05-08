package eval.fpp;

import eval.int128.Int128;
import eval.int128.Int128Funnel;
import eval.util.Slice;
import probfilter.crdt.immutable.GCuckooFilter;
import probfilter.pdsa.Filter;
import probfilter.pdsa.cuckoo.SimpleCuckooStrategy;


final class FppEvalGCF extends FppEval {
    private FppEvalGCF(Slice loadMagnitude) {
        super(loadMagnitude);
    }

    @Override
    protected Filter<Int128> createEmptyFilter(int capacity, short rid) {
        var strategy = SimpleCuckooStrategy.create(capacity, 4, 100, 8, new Int128Funnel());
        return new GCuckooFilter<>(strategy);
    }

    public static void main(String[] args) {
        var instance = new FppEvalGCF(Slice.fromTo(10, 20));
        instance.evalLocal("results/gcf_bs4_f8_1.00.csv");
        instance.evalSplit("results/gcf_bs4_f8_0.50.csv", .50);
        instance.evalSplit("results/gcf_bs4_f8_0.80.csv", .80);
        instance.evalSplit("results/gcf_bs4_f8_0.99.csv", .99);
    }
}

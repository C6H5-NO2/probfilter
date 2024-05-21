package eval.fpp;

import eval.int128.Int128;
import eval.int128.Int128Funnel;
import eval.util.Slice;
import probfilter.crdt.immutable.ScGCuckooFilter;
import probfilter.pdsa.Filter;
import probfilter.pdsa.cuckoo.SimpleCuckooStrategy;


final class FppEvalScGCF extends FppEval {
    private FppEvalScGCF(Slice loadMagnitude) {
        super(loadMagnitude);
    }

    @Override
    Filter<Int128> supplyFilter(int capacity, short rid) {
        var strategy = SimpleCuckooStrategy.create(capacity >> 2, 4, 100, 7, new Int128Funnel());
        return new ScGCuckooFilter<>(strategy);
    }

    public static void main(String[] args) {
        var instance = new FppEvalScGCF(Slice.fromTo(10, 20));
        instance.evalLocal("results/fpp/scgcf_bs4_f8_c4_1.00.csv");
        instance.evalSplit("results/fpp/scgcf_bs4_f8_c4_0.50.csv", .50);
        instance.evalSplit("results/fpp/scgcf_bs4_f8_c4_0.80.csv", .80);
        instance.evalSplit("results/fpp/scgcf_bs4_f8_c4_0.99.csv", .99);
    }
}

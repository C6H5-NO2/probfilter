package eval.fpp;

import eval.int128.Int128;
import eval.int128.Int128Funnel;
import eval.util.Slice;
import probfilter.crdt.immutable.ScORCuckooFilter;
import probfilter.pdsa.Filter;
import probfilter.pdsa.cuckoo.EntryStorageType;
import probfilter.pdsa.cuckoo.SimpleCuckooStrategy;


final class FppEvalScORCF extends FppEval {
    private FppEvalScORCF(Slice loadMagnitude) {
        super(loadMagnitude);
    }

    @Override
    Filter<Int128> supplyFilter(int capacity, short rid) {
        var strategy = SimpleCuckooStrategy.create(capacity >> 2, 4, 100, 7, EntryStorageType.VERSIONED_LONG, new Int128Funnel());
        return new ScORCuckooFilter<>(strategy, rid);
    }

    public static void main(String[] args) {
        var instance = new FppEvalScORCF(Slice.fromTo(10, 20));
        instance.evalLocal("results/fpp/scorcf_bs4_f8_c4_1.00.csv");
        instance.evalSplit("results/fpp/scorcf_bs4_f8_c4_0.50.csv", .50);
        instance.evalSplit("results/fpp/scorcf_bs4_f8_c4_0.80.csv", .80);
        instance.evalSplit("results/fpp/scorcf_bs4_f8_c4_0.99.csv", .99);
    }
}

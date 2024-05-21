package eval.mem;

import eval.int128.Int128;
import eval.int128.Int128Funnel;
import probfilter.crdt.mutable.GCuckooFilter;
import probfilter.pdsa.Filter;
import probfilter.pdsa.cuckoo.SimpleCuckooStrategy;


final class MemEvalGCF extends MemEval {
    private MemEvalGCF() {}

    @Override
    Filter<Int128> supplyFilter(int capacity, short rid) {
        var strategy = SimpleCuckooStrategy.create(capacity, 4, 100, 8, new Int128Funnel());
        return new GCuckooFilter<>(strategy);
    }

    public static void main(String[] args) {
        var instance = new MemEvalGCF();
        instance.evalLocalAdd100("results/mem/gcf_bs4_f8_add1.00_dist1.00.csv");
        instance.evalDist50Add100("results/mem/gcf_bs4_f8_add1.00_dist0.50.csv");
    }
}

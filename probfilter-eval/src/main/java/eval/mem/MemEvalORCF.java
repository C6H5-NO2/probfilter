package eval.mem;

import eval.int128.Int128;
import eval.int128.Int128Funnel;
import probfilter.crdt.mutable.ORCuckooFilter;
import probfilter.pdsa.Filter;
import probfilter.pdsa.cuckoo.EntryStorageType;
import probfilter.pdsa.cuckoo.SimpleCuckooStrategy;


final class MemEvalORCF extends MemEval {
    private MemEvalORCF() {}

    @Override
    Filter<Int128> supplyFilter(int capacity, short rid) {
        var strategy = SimpleCuckooStrategy.create(capacity, 4, 100, 8, EntryStorageType.VERSIONED_LONG, new Int128Funnel());
        return new ORCuckooFilter<>(strategy, rid);
    }

    public static void main(String[] args) {
        var instance = new MemEvalORCF();
        instance.evalLocalAdd100("results/mem/orcf_bs4_f8_add1.00_dist1.00.csv");
        instance.evalLocalAdd80("results/mem/orcf_bs4_f8_add0.80_dist1.00.csv");
        instance.evalLocalAdd51("results/mem/orcf_bs4_f8_add0.51_dist1.00.csv");
        instance.evalDistAdd100("results/mem/orcf_bs4_f8_add1.00_dist0.50.csv");
        instance.evalDistAdd80("results/mem/orcf_bs4_f8_add0.80_dist0.50.csv");
        instance.evalDistAdd51("results/mem/orcf_bs4_f8_add0.51_dist0.50.csv");
    }
}

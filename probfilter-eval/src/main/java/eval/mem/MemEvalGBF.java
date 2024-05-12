package eval.mem;

import eval.int128.Int128;
import eval.int128.Int128Funnel;
import probfilter.crdt.mutable.GBloomFilter;
import probfilter.pdsa.Filter;
import probfilter.pdsa.bloom.SimpleBloomStrategy;


final class MemEvalGBF extends MemEval {
    private MemEvalGBF() {}

    @Override
    Filter<Int128> supplyFilter(int capacity, short rid) {
        var strategy = SimpleBloomStrategy.create(capacity, 3e-2, new Int128Funnel());
        return new GBloomFilter<>(strategy);
    }

    public static void main(String[] args) {
        var instance = new MemEvalGBF();
        instance.evalLocalAdd100("results/mem/gbf_3e-2_add1.00_dist1.00.csv");
        instance.evalDistAdd100("results/mem/gbf_3e-2_add1.00_dist0.50.csv");
    }
}

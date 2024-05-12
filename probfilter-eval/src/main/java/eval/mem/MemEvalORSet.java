package eval.mem;

import eval.akka.AkkaORSet;
import eval.int128.Int128;
import probfilter.pdsa.Filter;


final class MemEvalORSet extends MemEval {
    private MemEvalORSet() {}

    @Override
    Filter<Int128> supplyFilter(int capacity, short rid) {
        return new AkkaORSet<>(rid);
    }

    public static void main(String[] args) {
        var instance = new MemEvalORSet();
        instance.evalLocalAdd100("results/mem/orset_add1.00_dist1.00.csv");
        instance.evalLocalAdd80("results/mem/orset_add0.80_dist1.00.csv");
        instance.evalLocalAdd51("results/mem/orset_add0.51_dist1.00.csv");
        instance.evalDistAdd100("results/mem/orset_add1.00_dist0.50.csv");
        instance.evalDistAdd80("results/mem/orset_add0.80_dist0.50.csv");
        instance.evalDistAdd51("results/mem/orset_add0.51_dist0.50.csv");
    }
}

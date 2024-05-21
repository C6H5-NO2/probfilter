package eval.mem;

import eval.akka.AkkaGSet;
import eval.int128.Int128;
import probfilter.pdsa.Filter;


final class MemEvalGSet extends MemEval {
    private MemEvalGSet() {}

    @Override
    Filter<Int128> supplyFilter(int capacity, short rid) {
        return new AkkaGSet<>();
    }

    public static void main(String[] args) {
        var instance = new MemEvalGSet();
        instance.evalLocalAdd100("results/mem/gset_add1.00_dist1.00.csv");
        instance.evalDist50Add100("results/mem/gset_add1.00_dist0.50.csv");
    }
}

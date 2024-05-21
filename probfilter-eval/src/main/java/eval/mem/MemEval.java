package eval.mem;

import eval.data.Dataset;
import eval.int128.Int128;
import probfilter.pdsa.Filter;


abstract class MemEval {
    private static final int LOAD_MAGNITUDE = Dataset.MAX_MAGNITUDE;
    private static final int REPEAT = Dataset.MAX_REPEAT;
    private static final double ADD_RATIO_80 = 0.80;
    private static final double ADD_RATIO_51 = 0.51;
    private static final double DIST_RATIO_50 = 0.50;

    MemEval() {}

    abstract Filter<Int128> supplyFilter(int capacity, short rid);

    final void evalLocalAdd100(String resultsPath) {
        var instance = new AddMemEval(LOAD_MAGNITUDE, REPEAT, this::supplyFilter);
        instance.eval(resultsPath);
    }

    final void evalLocalAdd80(String resultsPath) {
        var instance = new AddRmvMemEval(LOAD_MAGNITUDE, REPEAT, ADD_RATIO_80, this::supplyFilter);
        instance.eval(resultsPath);
    }

    final void evalLocalAdd51(String resultsPath) {
        var instance = new AddRmvMemEval(LOAD_MAGNITUDE, REPEAT, ADD_RATIO_51, this::supplyFilter);
        instance.eval(resultsPath);
    }

    final void evalDist50Add100(String resultsPath) {
        var instance = new Dist2AddMemEval(LOAD_MAGNITUDE, REPEAT, DIST_RATIO_50, this::supplyFilter);
        instance.eval(resultsPath);
    }

    final void evalDist50Add80(String resultsPath) {
        var instance = new Dist2AddRmvMemEval(LOAD_MAGNITUDE, REPEAT, ADD_RATIO_80, DIST_RATIO_50, this::supplyFilter);
        instance.eval(resultsPath);
    }

    final void evalDist50Add51(String resultsPath) {
        var instance = new Dist2AddRmvMemEval(LOAD_MAGNITUDE, REPEAT, ADD_RATIO_51, DIST_RATIO_50, this::supplyFilter);
        instance.eval(resultsPath);
    }
}

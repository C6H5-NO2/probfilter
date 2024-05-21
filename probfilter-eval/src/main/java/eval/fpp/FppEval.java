package eval.fpp;

import eval.data.Dataset;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.EvalLoop;
import eval.util.EvalRecord;
import eval.util.Filters;
import eval.util.Slice;
import probfilter.pdsa.Filter;

import java.io.BufferedWriter;


abstract class FppEval extends EvalLoop {
    private static final String[] CSV_HEADER_FIELDS = new String[]{
        "capacity",
        "theor fpp",
        "workload",
        "emp fpp",
        "load factor",
        "succ ins"
    };

    private final Int128Array data;

    protected FppEval(Slice loadMagnitude) {
        this(loadMagnitude, Dataset.MAX_REPEAT);
    }

    protected FppEval(Slice loadMagnitude, int repeat) {
        super(loadMagnitude, repeat, CSV_HEADER_FIELDS);
        if (loadMagnitude.until() > Dataset.MAX_MAGNITUDE + 1)
            throw new IllegalArgumentException();
        this.data = Dataset.acquire();
    }

    abstract Filter<Int128> supplyFilter(int capacity, short rid);

    protected final void evalLocal(String resultsPathname) {
        this.splitRatio = 1.0;
        eval(resultsPathname, true);
    }

    protected final void evalSplit(String resultsPathname, double splitRatio) {
        this.splitRatio = splitRatio;
        eval(resultsPathname, true);
    }

    private double splitRatio = 1.0;

    // i.e. abstract / override
    private boolean isLocal() {
        return splitRatio > 0.9999;
    }

    private Filters.FillResult loadFilter(int capacity, Int128Array data, int load, int epoch) {
        if (isLocal()) {
            var addSlice = Slice.fromLength(load * epoch, load);
            var filter = supplyFilter(capacity, (short) 1);
            return Filters.fill(filter, data, addSlice);
        } else {
            var tuple = Slice.fromLength(load * epoch, load).split(splitRatio);
            var addSlice1 = tuple._1;
            var addSlice2 = tuple._2;
            var filter1 = supplyFilter(capacity, (short) 1);
            var filter2 = supplyFilter(capacity, (short) 2);
            var result1 = Filters.fill(filter1, data, addSlice1);
            var result2 = Filters.fill(filter2, data, addSlice2);
            return result1.merge(result2);
        }
    }

    @Override
    protected final EvalRecord repeatStep(int load, int epoch, EvalRecord records) {
        //noinspection UnnecessaryLocalVariable
        int capacity = load;
        var fillResult = loadFilter(capacity, data, load, epoch);
        var filter = fillResult.filter();
        var tests = Slice.fromLength(Dataset.START_OF_TESTS + Dataset.LENGTH_OF_BATCH * epoch, Dataset.LENGTH_OF_BATCH);
        double empiricalFpp = Filters.measureFpp(filter, data, tests);
        double loadFactor = (double) filter.size() / capacity;
        double insertionRate = (double) fillResult.numSuccess() / load;
        return
            records
                .update("capacity", capacity)
                .update("theor fpp", filter.fpp())
                .update("workload", load)
                .append("emp fpp", empiricalFpp)
                .append("load factor", loadFactor)
                .append("succ ins", insertionRate);
    }

    @Override
    protected final void preLoadStep(BufferedWriter writer, int load) {
        int magnitude = Integer.numberOfTrailingZeros(load);
        if (isLocal()) {
            System.out.printf("load: local %d (2^%d)%n", load, magnitude);
        } else {
            System.out.printf("load: %.2f-dist-2 %d (2^%d)%n", splitRatio, load, magnitude);
        }
    }

    @Override
    protected final String postRepeatLoop(int load, EvalRecord records) {
        records = records.average();
        return String.format(
            "%d,%f,%d,%f,%f,%f",
            records.getInt("capacity"),
            records.getDouble("theor fpp"),
            records.getInt("workload"),
            records.getDouble("emp fpp"),
            records.getDouble("load factor"),
            records.getDouble("succ ins")
        );
    }
}

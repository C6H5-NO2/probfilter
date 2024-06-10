package eval.fpp;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.EvalLoop;
import eval.util.EvalRecord;
import eval.filter.Filters;
import eval.util.Slice;
import scala.Tuple2;


public sealed abstract class FppEvalLoop extends EvalLoop permits LocalFppEvalLoop, Distr2FppEvalLoop {
    private static final String[] CSV_HEADER_FIELDS = new String[]{
        "capacity",
        "theor fpp",
        "workload",
        "emp fpp",
        "load factor",
        "succ ins"
    };

    private final Int128Array data;

    protected FppEvalLoop(Slice loadMagnitudeRange, int repeat) {
        super(loadMagnitudeRange, repeat, CSV_HEADER_FIELDS);
        this.data = Dataset.acquire();
    }

    @Override
    protected final EvalRecord repeatStep(int variable, int epoch, EvalRecord records) {
        // variable is loadMagnitude
        int capacity = 1 << variable;
        int load = 1 << variable;
        var fillResult = loadFilter(capacity, data, load, epoch);
        var filter = fillResult._1;
        var sliceToTest = Slice.fromLength(
            Dataset.START_OF_TESTS + Dataset.LENGTH_OF_BATCH * epoch,
            Dataset.LENGTH_OF_BATCH
        );
        double empiricalFpp = Filters.measureEmpiricalFpp(filter, data, sliceToTest);
        double loadFactor = (double) filter.size() / capacity;
        double insSuccRate = (double) fillResult._2 / load;
        return
            records
                .updated("capacity", capacity)
                .updated("theor fpp", filter.fpp())
                .updated("workload", load)
                .appended("emp fpp", empiricalFpp)
                .appended("load factor", loadFactor)
                .appended("succ ins", insSuccRate);
    }

    @Override
    protected String postRepeatLoop(int variable, EvalRecord records) {
        records = records.averaged();
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

    protected abstract Tuple2<FluentCvRFilter<Int128>, Integer>
    loadFilter(int capacity, Int128Array data, int load, int epoch);
}

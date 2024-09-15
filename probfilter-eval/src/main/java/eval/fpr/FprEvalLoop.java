package eval.fpr;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.filter.Filters;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.EvalLoop;
import eval.util.EvalRecord;
import eval.util.Slice;


public sealed abstract class FprEvalLoop extends EvalLoop
    permits Distr2SyncPerFprLoop, Distr2SyncAllFprLoop, LocalSyncAllFprLoop {
    private static final String[] CSV_HEADER_FIELDS = new String[]{
        "capacity",
        "sync freq",
        "load factor",
        "theor fpr",
        "empir fpr"
    };

    private final int load;
    private final Int128Array data;

    protected FprEvalLoop(int loadMagnitude, int repeat, Slice syncFreqMagnitudeRange) {
        super(syncFreqMagnitudeRange, repeat, CSV_HEADER_FIELDS);
        this.load = 1 << loadMagnitude;
        this.data = Dataset.acquire();
    }

    @Override
    protected final EvalRecord repeatStep(int variable, int epoch, EvalRecord records) {
        // variable is syncFreqMagnitude
        int syncFreq = (int) Math.pow(10, variable);
        int capacity = this.load + 1 - 1;
        var filter = loadFilter(capacity, data, load, epoch, syncFreq);
        var sliceToTest = Slice.fromLength(
            Dataset.START_OF_TESTS + Dataset.LENGTH_OF_BATCH * epoch,
            Dataset.LENGTH_OF_BATCH
        );
        double empiricalFpp = Filters.measureEmpiricalFpp(filter, data, sliceToTest);
        double loadFactor = (double) filter.size() / capacity;
        return
            records
                .updated("capacity", capacity)
                .updated("sync freq", syncFreq)
                .appended("load factor", loadFactor)
                .updated("theor fpr", filter.fpp())
                .appended("empir fpr", empiricalFpp);
    }

    @Override
    protected String postRepeatLoop(int variable, EvalRecord records) {
        records = records.averaged();
        return String.format(
            "%d,%d,%f,%f,%f",
            records.getInt("capacity"),
            records.getInt("sync freq"),
            records.getDouble("load factor"),
            records.getDouble("theor fpr"),
            records.getDouble("empir fpr")
        );
    }

    protected abstract FluentCvRFilter<Int128>
    loadFilter(int capacity, Int128Array data, int load, int epoch, int syncFreq);
}

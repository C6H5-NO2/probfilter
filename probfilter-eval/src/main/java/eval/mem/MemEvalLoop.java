package eval.mem;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.data.Dataset;
import eval.filter.Filters;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.EvalLoop;
import eval.util.EvalRecord;
import eval.util.Slice;

import java.io.IOException;
import java.util.Scanner;


public sealed abstract class MemEvalLoop extends EvalLoop permits Distr2MemEvalLoop, LocalMemEvalLoop {
    private static final String[] CSV_HEADER_FIELDS = new String[]{
        "capacity",
        "size",
        "load factor",
        "retained bytes",
        "retained bpe",
        "serialized bytes",
        "serialized bpe"
    };

    private final int load;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final Scanner scanner;

    protected MemEvalLoop(int loadMagnitude, int repeat) {
        super(Slice.fromUntil(0, repeat), 1, CSV_HEADER_FIELDS);
        this.load = 1 << loadMagnitude;
        this.scanner = new Scanner(System.in);
    }

    @Override
    protected final EvalRecord repeatStep(int variable, int epoch, EvalRecord records) throws IOException {
        var data = Dataset.acquire();
        // `variable` is epoch, while `epoch` is always 0. `load` is fixed.
        int capacity = this.load + 1 - 1;
        var filter = loadFilter(capacity, data, this.load, variable);
        // data = null;
        // Dataset.release();
        // System.gc();
        // try {
        //     Thread.sleep(1500);
        // } catch (InterruptedException ignored) {
        //     ;
        // }
        // System.out.println("System.gc");
        // System.out.print("Enter retained size in bytes: ");
        // int retainedSize = scanner.nextInt();
        int retainedSize = 0;
        int entries = filter.size();
        double retainedBpe = (double) retainedSize / entries;
        int serializedSize = Filters.getSerializedSize(filter);
        double serializedBpe = (double) serializedSize / entries;
        return
            records
                .appended("capacity", capacity)
                .appended("size", entries)
                .appended("load factor", (double) entries / capacity)
                .appended("retained bytes", retainedSize)
                .appended("retained bpe", retainedBpe)
                .appended("serialized bytes", serializedSize)
                .appended("serialized bpe", serializedBpe);
    }

    @Override
    protected String postRepeatLoop(int variable, EvalRecord records) throws IOException {
        return String.format(
            "%d,%d,%f,%d,%f,%d,%f",
            records.getInt("capacity"),
            records.getInt("size"),
            records.getDouble("load factor"),
            records.getInt("retained bytes"),
            records.getDouble("retained bpe"),
            records.getInt("serialized bytes"),
            records.getDouble("serialized bpe")
        );
    }

    protected abstract FluentCvRFilter<Int128> loadFilter(int capacity, Int128Array data, int load, int epoch);
}

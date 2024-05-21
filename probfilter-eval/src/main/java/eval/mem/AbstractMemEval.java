package eval.mem;

import eval.data.Dataset;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.EvalLoop;
import eval.util.EvalRecord;
import eval.util.Filters;
import eval.util.Slice;
import probfilter.pdsa.Filter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Scanner;


public abstract class AbstractMemEval extends EvalLoop {
    private static final String[] CSV_HEADER_FIELDS = new String[]{
        "capacity",
        "size",
        "load factor",
        "retained bytes",
        "retained bpe",
        "serialized bytes",
        "serialized bpe"
    };

    private final Scanner scanner;

    protected AbstractMemEval(int loadMagnitude, int repeat) {
        super(Slice.fromTo(loadMagnitude, loadMagnitude), repeat, CSV_HEADER_FIELDS);
        this.scanner = new Scanner(System.in);
    }

    protected abstract Filter<Int128> loadFilter(int capacity, Int128Array data, int load, int epoch);

    private BufferedWriter writer = null;

    @Override
    protected final EvalRecord repeatStep(int load, int epoch, EvalRecord records) {
        //noinspection UnnecessaryLocalVariable
        int capacity = load;
        var data = Dataset.acquire();
        var filter = loadFilter(capacity, data, load, epoch);
        // data = null;
        // Dataset.release();
        System.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
            ;
        }
        // System.out.println("System.gc();");
        // System.out.print("Enter retained size in bytes: ");
        // int retainedSize = scanner.nextInt();
        int retainedSize = 0;
        int entries = filter.size();
        double retainedBpe = (double) retainedSize / entries;
        int serializedSize = Filters.getSerializedSize(filter);
        double serializedBpe = (double) serializedSize / entries;
        var result = String.format(
            "%d,%d,%f,%d,%f,%d,%f",
            capacity,
            entries,
            (double) entries / capacity,
            retainedSize,
            retainedBpe,
            serializedSize,
            serializedBpe
        );
        try {
            super.postLoadStep(this.writer, result, -1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return records.append("retained bpe", retainedBpe).append("serialized bpe", serializedBpe);
    }

    @Override
    protected final void preLoadLoop(BufferedWriter writer) throws IOException {
        super.preLoadLoop(writer);
        this.writer = writer;
    }

    @Override
    protected final void postLoadLoop(BufferedWriter writer) {
        this.writer = null;
    }

    @Override
    protected final void postLoadStep(BufferedWriter writer, String result, long milliseconds) {
        /* no-op */
    }

    @Override
    protected final EvalRecord preRepeatLoop(int load) {
        return new EvalRecord(new String[]{"retained bpe", "serialized bpe"});
    }

    @Override
    protected final String postRepeatLoop(int load, EvalRecord records) {
        records = records.average();
        System.out.printf("avgRetainedBpe=%f%n", records.getDouble("retained bpe"));
        System.out.printf("avgSerializedBpe=%f%n", records.getDouble("serialized bpe"));
        return "";
    }
}

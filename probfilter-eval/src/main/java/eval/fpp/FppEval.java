package eval.fpp;

import eval.data.Dataset;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.Filters;
import eval.util.Slice;
import eval.util.Timed;
import probfilter.crdt.Convergent;
import probfilter.pdsa.Filter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;


public abstract class FppEval {
    private static final String CSV_HEADER = "capacity,theor fpp,workload,emp fpp,load factor,succ ins";

    private final Int128Array data;
    private final Slice loadMagnitude;
    private final int repeat;

    protected FppEval(Slice loadMagnitude) {
        this(loadMagnitude, Dataset.MAX_REPEAT);
    }

    protected FppEval(Slice loadMagnitude, int repeat) {
        if (loadMagnitude.until() > Dataset.MAX_MAGNITUDE + 1)
            throw new IllegalArgumentException();
        this.data = Dataset.acquire();
        this.loadMagnitude = loadMagnitude;
        this.repeat = repeat;
    }

    protected abstract Filter<Int128> createEmptyFilter(int capacity, short rid);

    protected final void evalLocal(String resultsPathname) {
        var resultsPath = Path.of(resultsPathname);
        try (var writer = Files.newBufferedWriter(resultsPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND)) {
            writer.write(CSV_HEADER);
            writer.write('\n');
            for (int magnitude = loadMagnitude.from(); magnitude < loadMagnitude.until(); ++magnitude) {
                int load = 1 << magnitude;
                System.out.printf("load: local %d (2^%d)%n", load, magnitude);
                var result = Timed.measure(() -> evalLocalStep(load));
                writer.write(result._1);
                writer.write('\n');
                writer.flush();
                System.out.printf("results: %s%n", result._1);
                System.out.printf("time: %.3fs%n", result._2 / 1000.0);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String evalLocalStep(int load) {
        //noinspection UnnecessaryLocalVariable
        int capacity = load;
        double empFpp = 0;
        double loadFactor = 0;
        double succIns = 0;
        for (int epoch = 0; epoch < repeat; ++epoch) {
            var filter = createEmptyFilter(capacity, (short) 1);
            var tuple = Filters.fill(filter, data, Slice.fromLength(load * epoch, load));
            filter = tuple._1;
            succIns += (double) tuple._2 / load;
            loadFactor += (double) filter.size() / capacity;
            var tests = Slice.fromLength(Dataset.START_OF_TESTS + Dataset.LENGTH_OF_BATCH * epoch, Dataset.LENGTH_OF_BATCH);
            empFpp += Filters.measureFpp(filter, data, tests);
        }
        double theorFpp = createEmptyFilter(capacity, (short) 1).fpp();
        return String.format("%d,%f,%d,%f,%f,%f", capacity, theorFpp, load, empFpp / repeat, loadFactor / repeat, succIns / repeat);
    }

    protected final void evalSplit(String resultsPathname, double major) {
        if (major < .5 || major > 1)
            throw new IllegalArgumentException();
        var resultsPath = Path.of(resultsPathname);
        try (var writer = Files.newBufferedWriter(resultsPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND)) {
            writer.write(CSV_HEADER);
            writer.write('\n');
            for (int magnitude = loadMagnitude.from(); magnitude < loadMagnitude.until(); ++magnitude) {
                int load = 1 << magnitude;
                System.out.printf("load: %.2f-split %d (2^%d)%n", major, load, magnitude);
                var result = Timed.measure(() -> evalSplitStep(load, major));
                writer.write(result._1);
                writer.write('\n');
                writer.flush();
                System.out.printf("results: %s%n", result._1);
                System.out.printf("time: %.3fs%n", result._2 / 1000.0);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String evalSplitStep(int load, double major) {
        //noinspection UnnecessaryLocalVariable
        int capacity = load;
        double empFpp = 0;
        double loadFactor = 0;
        double succIns = 0;
        for (int epoch = 0; epoch < repeat; ++epoch) {
            var filter1 = createEmptyFilter(capacity, (short) 1);
            var src1 = Slice.fromLength(load * epoch, (int) (load * major));
            var tuple1 = Filters.fill(filter1, data, src1);
            filter1 = tuple1._1;
            var filter2 = createEmptyFilter(capacity, (short) 2);
            var src2 = Slice.fromUntil(src1.until(), load * (epoch + 1));
            var tuple2 = Filters.fill(filter2, data, src2);
            filter2 = tuple2._1;
            succIns += (double) (tuple1._2 + tuple2._2) / load;
            //noinspection unchecked
            var filter = ((Convergent<Filter<Int128>>) filter1).merge(filter2);
            loadFactor += (double) filter.size() / capacity;
            var tests = Slice.fromLength(Dataset.START_OF_TESTS + Dataset.LENGTH_OF_BATCH * epoch, Dataset.LENGTH_OF_BATCH);
            empFpp += Filters.measureFpp(filter, data, tests);
        }
        double theorFpp = createEmptyFilter(capacity, (short) 1).fpp();
        return String.format("%d,%f,%d,%f,%f,%f", capacity, theorFpp, load, empFpp / repeat, loadFactor / repeat, succIns / repeat);
    }
}

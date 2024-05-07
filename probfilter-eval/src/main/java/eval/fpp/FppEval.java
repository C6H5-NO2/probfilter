package eval.fpp;

import eval.data.Dataset;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.Slice;
import probfilter.crdt.Convergent;
import probfilter.pdsa.Filter;
import scala.Tuple2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;


abstract class FppEval {
    protected FppEval(Slice loadMagnitude) {
        if (loadMagnitude.until() > Dataset.MAX_MAGNITUDE + 1)
            throw new IllegalArgumentException();
        this.loadMagnitude = loadMagnitude;
        this.repeat = Dataset.MAX_REPEAT;
        this.data = Dataset.acquire();
    }

    private final Slice loadMagnitude;
    private final int repeat;
    private final Int128Array data;

    protected abstract Filter<Int128> createEmptyFilter(int capacity, int rid);

    protected final void evalLocal(String resultsPathname) {
        var resultsPath = Path.of(resultsPathname);
        try (var writer = Files.newBufferedWriter(resultsPath, StandardOpenOption.CREATE_NEW)) {
            writer.write("capacity, load, p(fp), p(fail)\n");
            for (int magnitude = loadMagnitude.from(); magnitude < loadMagnitude.until(); ++magnitude) {
                int load = 1 << magnitude;
                System.out.printf("load: local %d (2^%d)%n", load, magnitude);
                long startTime = System.currentTimeMillis();
                //noinspection UnnecessaryLocalVariable
                int capacity = load;
                double fpp = 0;
                double failp = 0;
                for (int epoch = 0; epoch < repeat; ++epoch) {
                    var filter = createEmptyFilter(capacity, 1);
                    var tuple = fill(filter, data, Slice.fromLength(load * epoch, load));
                    filter = tuple._1;
                    failp += (double) tuple._2 / load;
                    var tests = Slice.fromLength(Dataset.START_OF_TESTS + Dataset.LENGTH_OF_BATCH * epoch, Dataset.LENGTH_OF_BATCH);
                    fpp += testFpp(filter, data, tests);
                }
                var str = String.format("%d, %d, %f, %f", capacity, load, fpp / repeat, failp / repeat);
                writer.write(str);
                writer.write('\n');
                long endTime = System.currentTimeMillis();
                System.out.printf("results: %s%n", str);
                System.out.printf("time: %.3fs%n", (endTime - startTime) / 1000.0);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected final void evalSplit(String resultsPathname, double major) {
        if (major < .5 || major > 1)
            throw new IllegalArgumentException();
        var resultsPath = Path.of(resultsPathname);
        try (var writer = Files.newBufferedWriter(resultsPath, StandardOpenOption.CREATE_NEW)) {
            writer.write("capacity, load, p(fp), p(fail)\n");
            for (int magnitude = loadMagnitude.from(); magnitude < loadMagnitude.until(); ++magnitude) {
                int load = 1 << magnitude;
                System.out.printf("load: %f-split %d (2^%d)%n", major, load, magnitude);
                long startTime = System.currentTimeMillis();
                //noinspection UnnecessaryLocalVariable
                int capacity = load;
                double fpp = 0;
                double failp = 0;
                for (int epoch = 0; epoch < repeat; ++epoch) {
                    var filter1 = createEmptyFilter(capacity, 1);
                    var src1 = Slice.fromLength(load * epoch, (int) (load * major));
                    var tuple1 = fill(filter1, data, src1);
                    filter1 = tuple1._1;
                    var filter2 = createEmptyFilter(capacity, 2);
                    var tuple2 = fill(filter2, data, Slice.fromUntil(src1.until(), load * (epoch + 1)));
                    filter2 = tuple2._1;
                    failp += (double) (tuple1._2 + tuple2._2) / load;
                    //noinspection unchecked
                    var filter = ((Convergent<Filter<Int128>>) filter1).merge(filter2);
                    var tests = Slice.fromLength(Dataset.START_OF_TESTS + Dataset.LENGTH_OF_BATCH * epoch, Dataset.LENGTH_OF_BATCH);
                    fpp += testFpp(filter, data, tests);
                }
                var str = String.format("%d, %d, %f, %f", capacity, load, fpp / repeat, failp / repeat);
                writer.write(str);
                writer.write('\n');
                long endTime = System.currentTimeMillis();
                System.out.printf("results: %s%n", str);
                System.out.printf("time: %.3fs%n", (endTime - startTime) / 1000.0);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Tuple2<Filter<Int128>, Integer> fill(Filter<Int128> filter, Int128Array data, Slice src) {
        int numFailure = 0;
        for (int i = src.from(); i < src.until(); ++i) {
            var int128 = data.get(i);
            var result = filter.tryAdd(int128);
            if (result.isSuccess()) {
                filter = result.get();
            } else {
                ++numFailure;
            }
        }
        return new Tuple2<>(filter, numFailure);
    }

    private static double testFpp(Filter<Int128> filter, Int128Array data, Slice tests) {
        int numFP = 0;
        for (int i = tests.from(); i < tests.until(); ++i) {
            var int128 = data.get(i);
            boolean result = filter.contains(int128);
            if (result) {
                ++numFP;
            }
        }
        return (double) numFP / tests.length();
    }
}

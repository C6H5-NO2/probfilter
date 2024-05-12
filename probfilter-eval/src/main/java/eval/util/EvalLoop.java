package eval.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;


public abstract class EvalLoop {
    private final Slice loadMagnitude;
    private final int repeat;
    private final String csvHeader;

    protected EvalLoop(Slice loadMagnitude, int repeat, String... headerFields) {
        this.loadMagnitude = loadMagnitude;
        this.repeat = repeat;
        this.csvHeader = String.join(",", headerFields);
    }

    public final void eval(String resultsPathname) {
        eval(resultsPathname, false);
    }

    public final void eval(String resultsPathname, boolean timed) {
        var resultsPath = Path.of(resultsPathname);
        try (var writer = Files.newBufferedWriter(resultsPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND)) {
            preLoadLoop(writer);
            for (int magnitude = loadMagnitude.from(); magnitude < loadMagnitude.until(); ++magnitude) {
                int load = 1 << magnitude;
                preLoadStep(writer, load);
                if (timed) {
                    var result = Timed.measure(() -> loadStep(load));
                    postLoadStep(writer, result._1, result._2);
                } else {
                    var result = loadStep(load);
                    postLoadStep(writer, result, -1);
                }
            }
            postLoadLoop(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String loadStep(int load) {
        var records = preRepeatLoop(load);
        for (int epoch = 0; epoch < repeat; ++epoch) {
            records = repeatStep(load, epoch, records);
        }
        return postRepeatLoop(load, records);
    }

    protected abstract EvalRecord repeatStep(int load, int epoch, EvalRecord records);

    protected void preLoadLoop(BufferedWriter writer) throws IOException {
        writer.write(csvHeader);
        writer.write('\n');
    }

    protected void postLoadLoop(BufferedWriter writer) throws IOException {}

    protected void preLoadStep(BufferedWriter writer, int load) throws IOException {}

    protected void postLoadStep(BufferedWriter writer, String result, long milliseconds) throws IOException {
        writer.write(result);
        writer.write('\n');
        writer.flush();
        System.out.printf("results: %s%n", result);
        if (milliseconds > 0)
            System.out.printf("time: %.3fs%n", milliseconds / 1000.0);
    }

    protected EvalRecord preRepeatLoop(int load) {
        return null;
    }

    protected String postRepeatLoop(int load, EvalRecord records) {
        return "";
    }
}

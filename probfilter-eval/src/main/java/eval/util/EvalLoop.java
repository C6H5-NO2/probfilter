package eval.util;

import com.google.common.base.Strings;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;


public abstract class EvalLoop {
    private final Slice varRange;
    private final int repeat;
    private final String[] headerFields;
    protected Writer writer;

    protected EvalLoop(Slice varRange, int repeat, String... headerFields) {
        this.varRange = varRange;
        this.repeat = repeat;
        this.headerFields = headerFields;
        this.writer = new NullDeviceWriter();
    }

    public final void eval(String saveTo) {
        eval(saveTo, false);
    }

    public final void eval(String saveTo, boolean timed) {
        try (
            var writer =
                Strings.isNullOrEmpty(saveTo) ?
                    new NullDeviceWriter() :
                    Files.newBufferedWriter(Path.of(saveTo), StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND)
        ) {
            this.writer = writer;
            preVarLoop();
            for (int variable = varRange.from(); variable < varRange.until(); ++variable) {
                preVarStep(variable);
                if (timed) {
                    int finalVar = variable;
                    var result = Timed.measure(() -> {
                        try {
                            return varStep(finalVar);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    postVarStep(variable, result._1, result._2);
                } else {
                    var result = varStep(variable);
                    postVarStep(variable, result, -1);
                }
            }
            postVarLoop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            this.writer = null;
        }
    }

    private String varStep(int variable) throws IOException {
        var records = preRepeatLoop(variable);
        for (int epoch = 0; epoch < repeat; ++epoch) {
            records = repeatStep(variable, epoch, records);
        }
        return postRepeatLoop(variable, records);
    }

    protected void preVarLoop() throws IOException {
        var header = String.join(",", headerFields);
        writer.write(header);
        writer.write('\n');
        writer.flush();
        System.out.println(header);
    }

    protected void postVarLoop() throws IOException {}

    protected void preVarStep(int variable) throws IOException {
        System.out.printf("with variable = %s%n", variable);
    }

    /**
     * @param milliseconds Pass a negative number to suppress printing time.
     */
    protected void postVarStep(int variable, String result, long milliseconds) throws IOException {
        writer.write(result);
        writer.write('\n');
        writer.flush();
        System.out.printf("results: %s%n", result);
        if (milliseconds > 0)
            System.out.printf("finished in %.3f seconds%n", milliseconds / 1000.0);
    }

    protected EvalRecord preRepeatLoop(int variable) throws IOException {
        return new EvalRecord(headerFields);
    }

    protected abstract EvalRecord repeatStep(int variable, int epoch, EvalRecord records) throws IOException;

    protected String postRepeatLoop(int variable, EvalRecord records) throws IOException {
        records = records.averaged();
        int length = headerFields.length;
        var format = String.join(",", Collections.nCopies(length, "%s"));
        var args = new Object[length];
        for (int i = 0; i < length; ++i) {
            args[i] = records.get(headerFields[i]);
        }
        return String.format(format, args);
    }

    private final static class NullDeviceWriter extends Writer {
        @Override
        @SuppressWarnings("NullableProblems")
        public void write(char[] cbuf, int off, int len) throws IOException {}

        @Override
        public void flush() throws IOException {}

        @Override
        public void close() throws IOException {}
    }
}

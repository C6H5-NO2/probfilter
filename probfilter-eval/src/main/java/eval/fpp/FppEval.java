package eval.fpp;

import eval.data.Dataset;
import eval.filter.FilterConfig;
import eval.filter.FilterSupplier;
import eval.util.EvalLoop;
import eval.util.Slice;

import java.util.List;


public final class FppEval {
    private static final Slice LOAD_MAGNITUDE_RANGE = Slice.fromTo(10, Dataset.MAX_MAGNITUDE);
    private static final int REPEAT = Dataset.MAX_REPEAT;
    private static final double DISTR_RATIO_50 = 0.50;
    private static final double DISTR_RATIO_80 = 0.80;
    private static final double DISTR_RATIO_99 = 0.99;

    private final String evalId;
    private final FilterSupplier supplier;

    public FppEval(String evalId, FilterSupplier supplier) {
        this.evalId = evalId;
        this.supplier = supplier;
    }

    public void evalAll() {
        EvalLoop loop = new LocalFppEvalLoop(LOAD_MAGNITUDE_RANGE, REPEAT, supplier);
        loop.eval(String.format("results/fpp/%s_1.00.csv", evalId), true);

        loop = new Distr2FppEvalLoop(LOAD_MAGNITUDE_RANGE, REPEAT, DISTR_RATIO_50, supplier);
        loop.eval(String.format("results/fpp/%s_0.50.csv", evalId), true);

        loop = new Distr2FppEvalLoop(LOAD_MAGNITUDE_RANGE, REPEAT, DISTR_RATIO_80, supplier);
        loop.eval(String.format("results/fpp/%s_0.80.csv", evalId), true);

        loop = new Distr2FppEvalLoop(LOAD_MAGNITUDE_RANGE, REPEAT, DISTR_RATIO_99, supplier);
        loop.eval(String.format("results/fpp/%s_0.99.csv", evalId), true);
    }

    public static void main(String[] args) {
        var configs = List.of(
            FilterConfig.MUT_GBF,
            FilterConfig.IMM_GCF_1,
            FilterConfig.IMM_GCF_4,
            FilterConfig.IMM_ORCF_1,
            FilterConfig.IMM_ORCF_4,
            FilterConfig.MUT_SCGBF,
            FilterConfig.IMM_SCGCF,
            FilterConfig.IMM_SCORCF
        );
        for (var config : configs) {
            var instance = new FppEval(config.nameId(), config.supplier());
            instance.evalAll();
        }
    }
}

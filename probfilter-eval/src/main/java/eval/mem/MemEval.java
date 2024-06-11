package eval.mem;

import eval.data.Dataset;
import eval.filter.FilterConfig;
import eval.util.EvalLoop;

import java.util.List;


public final class MemEval {
    private static final int LOAD_MAGNITUDE = Dataset.MAX_MAGNITUDE;
    private static final int REPEAT = Dataset.MAX_REPEAT;
    private static final double ADD_RATIO_100 = 1.00;
    private static final double ADD_RATIO_80 = 0.80;
    private static final double ADD_RATIO_51 = 0.51;
    private static final double DISTR_RATIO_50 = 0.50;

    private final FilterConfig config;

    public MemEval(FilterConfig config) {
        this.config = config;
    }

    public void evalAll(boolean removable) {
        EvalLoop loop = new LocalMemEvalLoop(LOAD_MAGNITUDE, REPEAT, ADD_RATIO_100, config.supplier());
        loop.eval(String.format("results/mem/%s_add1.00_distr1.00.csv", config.nameId()));

        loop = new Distr2MemEvalLoop(LOAD_MAGNITUDE, REPEAT, ADD_RATIO_100, DISTR_RATIO_50, config.supplier());
        loop.eval(String.format("results/mem/%s_add1.00_distr0.50.csv", config.nameId()));

        if (!removable) {
            return;
        }

        new LocalMemEvalLoop(LOAD_MAGNITUDE, REPEAT, ADD_RATIO_80, config.supplier());
        loop.eval(String.format("results/mem/%s_add0.80_distr1.00.csv", config.nameId()));

        loop = new Distr2MemEvalLoop(LOAD_MAGNITUDE, REPEAT, ADD_RATIO_80, DISTR_RATIO_50, config.supplier());
        loop.eval(String.format("results/mem/%s_add0.80_distr0.50.csv", config.nameId()));

        new LocalMemEvalLoop(LOAD_MAGNITUDE, REPEAT, ADD_RATIO_51, config.supplier());
        loop.eval(String.format("results/mem/%s_add0.51_distr1.00.csv", config.nameId()));

        loop = new Distr2MemEvalLoop(LOAD_MAGNITUDE, REPEAT, ADD_RATIO_51, DISTR_RATIO_50, config.supplier());
        loop.eval(String.format("results/mem/%s_add0.51_distr0.50.csv", config.nameId()));
    }

    public static void main(String[] args) {
        var configs = List.of(
            FilterConfig.MUT_ORCF_4,
            FilterConfig.IMM_ORSET,
            FilterConfig.MUT_GBF,
            FilterConfig.MUT_GCF_4,
            FilterConfig.IMM_GSET
        );
        for (int i = 0; i < configs.size(); ++i) {
            boolean removable = i < 2;
            new MemEval(configs.get(i)).evalAll(removable);
        }
    }
}

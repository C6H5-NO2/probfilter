package eval.lat;

import eval.data.Dataset;
import eval.filter.FilterConfig;
import eval.util.EvalLoop;
import eval.util.Slice;

import java.util.List;


public final class LatEval {
    private static final int LOAD_MAGNITUDE = Dataset.MAX_MAGNITUDE;
    private static final int REPEAT = Dataset.MAX_REPEAT;
    private static final double ADD_RATIO_100 = 1.00;
    private static final double ADD_RATIO_80 = 0.80;
    private static final double ADD_RATIO_51 = 0.51;
    private static final Slice SYNC_FREQ_MAGNITUDE_RANGE = Slice.fromTo(0, 6);

    private final FilterConfig config;

    public LatEval(FilterConfig config) {
        this.config = config;
    }

    public void evalAll(boolean removable) {
        EvalLoop loop = new Distr2LatEvalLoop(LOAD_MAGNITUDE, REPEAT, SYNC_FREQ_MAGNITUDE_RANGE, ADD_RATIO_100, config.supplier());
        loop.eval(String.format("results/lat/%s_add1.00.csv", config.nameId()));

        if (!removable) {
            return;
        }

        loop = new Distr2LatEvalLoop(LOAD_MAGNITUDE, REPEAT, SYNC_FREQ_MAGNITUDE_RANGE, ADD_RATIO_80, config.supplier());
        loop.eval(String.format("results/lat/%s_add0.80.csv", config.nameId()));

        loop = new Distr2LatEvalLoop(LOAD_MAGNITUDE, REPEAT, SYNC_FREQ_MAGNITUDE_RANGE, ADD_RATIO_51, config.supplier());
        loop.eval(String.format("results/lat/%s_add0.51.csv", config.nameId()));
    }

    public static void main(String[] args) {
        var configs = List.of(
            FilterConfig.MUT_ORCF_4,
            FilterConfig.MUT_GCF_4
        );
        for (int i = 0; i < configs.size(); ++i) {
            boolean removable = i < 1;
            new LatEval(configs.get(i)).evalAll(removable);
        }
    }
}

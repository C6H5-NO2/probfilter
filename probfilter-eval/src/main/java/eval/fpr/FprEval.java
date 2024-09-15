package eval.fpr;

import eval.data.Dataset;
import eval.filter.FilterConfig;
import eval.util.Slice;

import java.util.List;


public final class FprEval {
    private static final int LOAD_MAGNITUDE = Dataset.MAX_MAGNITUDE;
    private static final int REPEAT = Dataset.MAX_REPEAT;
    private static final double DISTR_RATIO_99 = 0.99;
    private static final double DISTR_RATIO_80 = 0.80;
    private static final double DISTR_RATIO_50 = 0.50;
    private static final Slice SYNC_FREQ_MAGNITUDE_RANGE = Slice.fromTo(3, 7);

    private final FilterConfig config;

    public FprEval(FilterConfig config) {
        this.config = config;
    }

    public void evalAll() {
        System.out.printf("eval %s%n", config.nameId());

        new LocalSyncAllFprLoop(LOAD_MAGNITUDE, REPEAT, config.supplier())
            .eval(String.format("results/fpr/%s_add1.00_distr1.00_synced.csv", config.nameId()), true);

        new Distr2SyncAllFprLoop(LOAD_MAGNITUDE, REPEAT, DISTR_RATIO_99, config.supplier())
            .eval(String.format("results/fpr/%s_add1.00_distr0.99_synced.csv", config.nameId()), true);

        new Distr2SyncAllFprLoop(LOAD_MAGNITUDE, REPEAT, DISTR_RATIO_80, config.supplier())
            .eval(String.format("results/fpr/%s_add1.00_distr0.80_synced.csv", config.nameId()), true);

        new Distr2SyncAllFprLoop(LOAD_MAGNITUDE, REPEAT, DISTR_RATIO_50, config.supplier())
            .eval(String.format("results/fpr/%s_add1.00_distr0.50_synced.csv", config.nameId()), true);

        new Distr2SyncPerFprLoop(LOAD_MAGNITUDE, REPEAT, SYNC_FREQ_MAGNITUDE_RANGE, DISTR_RATIO_99, config.supplier())
            .eval(String.format("results/fpr/%s_add1.00_distr0.99_lat.csv", config.nameId()), true);

        new Distr2SyncPerFprLoop(LOAD_MAGNITUDE, REPEAT, SYNC_FREQ_MAGNITUDE_RANGE, DISTR_RATIO_80, config.supplier())
            .eval(String.format("results/fpr/%s_add1.00_distr0.80_lat.csv", config.nameId()), true);

        new Distr2SyncPerFprLoop(LOAD_MAGNITUDE, REPEAT, SYNC_FREQ_MAGNITUDE_RANGE, DISTR_RATIO_50, config.supplier())
            .eval(String.format("results/fpr/%s_add1.00_distr0.50_lat.csv", config.nameId()), true);
    }

    public static void main(String[] args) {
        var configs = List.of(
            FilterConfig.MUT_GBF,
            FilterConfig.IMM_GCF_4,
            FilterConfig.IMM_ORCF_4,
            FilterConfig.MUT_SCGBF,
            FilterConfig.IMM_SCGCF,
            FilterConfig.IMM_SCORCF
        );
        for (var config : configs) {
            new FprEval(config).evalAll();
        }
    }
}

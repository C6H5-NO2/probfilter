package eval.filter;

import com.c6h5no2.probfilter.crdt.*;
import com.c6h5no2.probfilter.pdsa.bloom.SimpleBloomStrategy;
import com.c6h5no2.probfilter.pdsa.cuckoo.CuckooEntryType;
import com.c6h5no2.probfilter.pdsa.cuckoo.SimpleCuckooStrategy;
import eval.int128.Int128;
import eval.int128.Int128Funnel;


public enum FilterConfig {
    MUT_GBF("gbf_3e-2", (capacity, rid) -> {
        var strategy = SimpleBloomStrategy.apply(capacity, 3e-2, Int128Funnel.INSTANCE);
        return GBloomFilter.apply(true, strategy).asFluent();
    }),

    IMM_GCF_1("gcf_bs1_f8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 1, 500, 8, Int128Funnel.INSTANCE);
        return GCuckooFilter.apply(false, strategy, 0).asFluent();
    }),

    MUT_GCF_1("gcf_bs1_f8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 1, 500, 8, Int128Funnel.INSTANCE);
        return GCuckooFilter.apply(true, strategy, 0).asFluent();
    }),

    IMM_GCF_4("gcf_bs4_f8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 4, 500, 8, Int128Funnel.INSTANCE);
        return GCuckooFilter.apply(false, strategy, 0).asFluent();
    }),

    MUT_GCF_4("gcf_bs4_f8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 4, 500, 8, Int128Funnel.INSTANCE);
        return GCuckooFilter.apply(true, strategy, 0).asFluent();
    }),

    IMM_ORCF_1("orcf_bs1_f8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 1, 500, 8, CuckooEntryType.VERSIONED_LONG, Int128Funnel.INSTANCE);
        return ORCuckooFilter.apply(false, strategy, rid, rid).asFluent();
    }),

    MUT_ORCF_1("orcf_bs1_f8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 1, 500, 8, CuckooEntryType.VERSIONED_LONG, Int128Funnel.INSTANCE);
        return ORCuckooFilter.apply(true, strategy, rid, rid).asFluent();
    }),

    IMM_ORCF_4("orcf_bs4_f8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 4, 500, 8, CuckooEntryType.VERSIONED_LONG, Int128Funnel.INSTANCE);
        return ORCuckooFilter.apply(false, strategy, rid, rid).asFluent();
    }),

    MUT_ORCF_4("orcf_bs4_f8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 4, 500, 8, CuckooEntryType.VERSIONED_LONG, Int128Funnel.INSTANCE);
        return ORCuckooFilter.apply(true, strategy, rid, rid).asFluent();
    }),

    MUT_SCGBF("scgbf_3e-2_c4", (capacity, rid) -> {
        var strategy = SimpleBloomStrategy.apply(capacity >> 2, 3e-2, Int128Funnel.INSTANCE);
        return ScGBloomFilter.apply(true, strategy).asFluent();
    }),

    IMM_SCGCF("scgcf_bs4_f8_c4", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity >> 2, 4, 500, 8, Int128Funnel.INSTANCE);
        return ScGCuckooFilter.apply(false, strategy, 0).asFluent();
    }),

    IMM_SCORCF("scorcf_bs4_f8_c4", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity >> 2, 4, 500, 8, CuckooEntryType.VERSIONED_LONG, Int128Funnel.INSTANCE);
        return ScORCuckooFilter.apply(false, strategy, rid, rid).asFluent();
    }),

    IMM_GSET("gset", (capacity, rid) -> new AkkaGSet<Int128>().asFluent()),

    IMM_ORSET("orset", (capacity, rid) -> new AkkaORSet<Int128>(rid).asFluent());

    private final String _nameId;
    private final FilterSupplier _supplier;

    FilterConfig(String nameId, FilterSupplier supplier) {
        this._nameId = nameId;
        this._supplier = supplier;
    }

    public String nameId() {
        return _nameId;
    }

    public FilterSupplier supplier() {
        return _supplier;
    }
}

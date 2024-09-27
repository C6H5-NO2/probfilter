package eval.filter;

import com.c6h5no2.probfilter.crdt.*;
import com.c6h5no2.probfilter.pdsa.bloom.SimpleBloomStrategy;
import com.c6h5no2.probfilter.pdsa.cuckoo.CuckooEntryType;
import com.c6h5no2.probfilter.pdsa.cuckoo.SimpleCuckooStrategy;
import eval.int128.Int128;
import eval.int128.Int128Funnel;


public enum FilterConfig {
    MUT_GBF("gbf_2p-5", (capacity, rid) -> {
        var strategy = SimpleBloomStrategy.apply(capacity, 0.03125, Int128Funnel.INSTANCE);
        return GBloomFilter.apply(true, strategy).asFluent();
    }),

    IMM_GCF_4("gcf_c4_l8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 4, 500, 8, Int128Funnel.INSTANCE);
        return GCuckooFilter.apply(false, strategy, rid).asFluent();
    }),

    MUT_GCF_4("gcf_c4_l8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 4, 500, 8, Int128Funnel.INSTANCE);
        return GCuckooFilter.apply(true, strategy, rid).asFluent();
    }),

    IMM_ORCF_4("orcf_c4_l8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 4, 500, 8, CuckooEntryType.VERSIONED_LONG, Int128Funnel.INSTANCE);
        return ORCuckooFilter.apply(false, strategy, rid, rid).asFluent();
    }),

    MUT_ORCF_4("orcf_c4_l8", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity, 4, 500, 8, CuckooEntryType.VERSIONED_LONG, Int128Funnel.INSTANCE);
        return ORCuckooFilter.apply(true, strategy, rid, rid).asFluent();
    }),

    MUT_SCGBF("scgbf_2p-5_s4", (capacity, rid) -> {
        var strategy = SimpleBloomStrategy.apply(capacity >> 2, 0.03125, Int128Funnel.INSTANCE);
        return ScGBloomFilter.apply(true, strategy).asFluent();
    }),

    IMM_SCGCF("scgcf_c4_l8_s4", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity >> 2, 4, 500, 8, Int128Funnel.INSTANCE);
        return ScGCuckooFilter.apply(false, strategy, rid).asFluent();
    }),

    MUT_SCGCF("scgcf_c4_l8_s4", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity >> 2, 4, 500, 8, Int128Funnel.INSTANCE);
        return ScGCuckooFilter.apply(true, strategy, rid).asFluent();
    }),

    IMM_SCORCF("scorcf_c4_l8_s4", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity >> 2, 4, 500, 8, CuckooEntryType.VERSIONED_LONG, Int128Funnel.INSTANCE);
        return ScORCuckooFilter.apply(false, strategy, rid, rid).asFluent();
    }),

    MUT_SCORCF("scorcf_c4_l8_s4", (capacity, rid) -> {
        var strategy = SimpleCuckooStrategy.apply(capacity >> 2, 4, 500, 8, CuckooEntryType.VERSIONED_LONG, Int128Funnel.INSTANCE);
        return ScORCuckooFilter.apply(true, strategy, rid, rid).asFluent();
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

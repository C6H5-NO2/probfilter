package eval.lat;

import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import com.c6h5no2.probfilter.util.Mutable;
import eval.int128.Int128;
import eval.int128.Int128Array;
import eval.util.OffsetBitSet;
import eval.util.Slice;

import java.util.Random;


/**
 * A mutable wrapper for filter whose operations are tracked. Only suitable for tracking 2 filters casually.
 * The underlying filter can be immutable or mutable.
 */
public final class TrackedFilter implements Mutable {
    private FluentCvRFilter<Int128> filter;
    private final Int128Array data;
    private final Slice sliceToAdd;
    private int indexToAdd;
    private boolean full;
    private final double addRatio;
    private final Random rng;
    private final OffsetBitSet contained, concurAdded, concurRemoved;
    private int numAppliedOps, numFalseNegatives;

    public TrackedFilter(
        FluentCvRFilter<Int128> empty,
        Int128Array data,
        Slice sliceToAdd,
        double addRatio,
        Random rng,
        Slice roi
    ) {
        this.filter = empty;
        this.data = data;
        this.sliceToAdd = sliceToAdd;
        this.indexToAdd = sliceToAdd.from();
        this.full = false;
        this.addRatio = addRatio;
        this.rng = rng;
        this.contained = new OffsetBitSet(roi.until(), roi.from());
        this.concurAdded = new OffsetBitSet(roi.until(), roi.from());
        this.concurRemoved = new OffsetBitSet(roi.until(), roi.from());
        this.numAppliedOps = 0;
        this.numFalseNegatives = 0;
    }

    public TrackedFilter step() {
        if (rng.nextDouble() < addRatio || contained.cardinality() == 0) {
            // add
            if (full) {
                throw new RuntimeException();
            }
            if (indexToAdd > sliceToAdd.until()) {
                throw new RuntimeException();
            }
            var elem = data.get(indexToAdd);
            var result = filter.tryAdd(elem);
            if (result.isSuccess()) {
                filter = result.get();
                contained.set(indexToAdd);
                concurAdded.set(indexToAdd);
                concurRemoved.clear(indexToAdd);
                indexToAdd++;
            } else {
                full = true;
            }
        } else {
            // remove
            int indexInStream = rng.nextInt(contained.cardinality());
            // noinspection OptionalGetWithoutIsPresent
            int indexToRmv = contained.stream().skip(indexInStream).findFirst().getAsInt();
            var elem = data.get(indexToRmv);
            if (!filter.contains(elem)) {
                // false negative
                numFalseNegatives++;
            }
            filter = filter.remove(elem);
            contained.clear(indexToRmv);
            concurAdded.clear(indexToRmv);
            concurRemoved.set(indexToRmv);
        }
        numAppliedOps++;
        return this;
    }

    /**
     * @apiNote Only works for causal merge of 2 filters.
     */
    public TrackedFilter merge(TrackedFilter that) {
        this.filter = this.filter.merge(that.filter);
        var added = that.concurAdded.or(this.concurAdded);
        var removed = that.concurRemoved.or(this.concurRemoved);
        this.contained.or(that.contained).andNot(removed).or(added);
        that.concurAdded.clear();
        that.concurRemoved.clear();
        return this;
    }

    public FluentCvRFilter<Int128> getFilter() {
        return filter;
    }

    public boolean isFull() {
        return full;
    }

    public int getNumAppliedOps() {
        return numAppliedOps;
    }

    public int getNumFalseNegatives() {
        return numFalseNegatives;
    }
}

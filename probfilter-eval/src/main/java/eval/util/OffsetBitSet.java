package eval.util;

import java.util.BitSet;
import java.util.stream.IntStream;


/**
 * A mutable bit set that always contains {@code false} ({@code 0}) until an offset.
 */
public final class OffsetBitSet {
    private final BitSet bitset;
    public final int offset;
    private int activeBits;

    @Deprecated
    public OffsetBitSet(int capacity) {
        this(capacity, 0);
    }

    /**
     * @param capacity the same value as {@code nbits} in {@link java.util.BitSet#BitSet(int)}
     * @param offset {@code false} until this offset
     */
    public OffsetBitSet(int capacity, int offset) {
        this(new BitSet(capacity - offset), offset, 0);
    }

    private OffsetBitSet(BitSet bitset, int offset, int activeBits) {
        this.bitset = bitset;
        this.offset = offset;
        this.activeBits = activeBits;
    }

    /**
     * @implNote O(1)-ish rather than O(n)
     * @see java.util.BitSet#cardinality()
     */
    public int cardinality() {
        // return bitset.cardinality();
        if (activeBits < 0)
            activeBits = bitset.cardinality();
        return activeBits;
    }

    /**
     * @implNote O(1)
     * @see java.util.BitSet#length()
     */
    public int length() {
        return offset + bitset.length();
    }

    /**
     * @see java.util.BitSet#get(int)
     */
    public boolean get(int index) {
        return bitset.get(index - offset);
    }

    /**
     * @see java.util.BitSet#clear()
     */
    public OffsetBitSet clear() {
        activeBits = 0;
        bitset.clear();
        return this;
    }

    /**
     * @see java.util.BitSet#clear(int)
     */
    public OffsetBitSet clear(int index) {
        if (get(index)) {
            activeBits = cardinality() - 1;
            bitset.clear(index - offset);
        }
        return this;
    }

    /**
     * @see java.util.BitSet#set(int)
     */
    public OffsetBitSet set(int index) {
        if (!get(index)) {
            activeBits = cardinality() + 1;
            bitset.set(index - offset);
        }
        return this;
    }

    /**
     * @see java.util.BitSet#set(int, int)
     */
    public OffsetBitSet set(int from, int until) {
        bitset.set(from - offset, until - offset);
        activeBits = -1;
        return this;
    }

    /**
     * @see java.util.BitSet#or(BitSet)
     */
    public OffsetBitSet or(OffsetBitSet that) {
        if (this.offset != that.offset)
            throw new IllegalArgumentException();
        this.bitset.or(that.bitset);
        activeBits = -1;
        return this;
    }

    /**
     * {@code this &= ~that}
     *
     * @see java.util.BitSet#andNot(BitSet)
     */
    public OffsetBitSet andNot(OffsetBitSet that) {
        if (this.offset != that.offset)
            throw new IllegalArgumentException();
        this.bitset.andNot(that.bitset);
        activeBits = -1;
        return this;
    }

    /**
     * @see java.util.BitSet#stream()
     */
    public IntStream stream() {
        return bitset.stream().map(i -> offset + i);
    }

    public OffsetBitSet copy() {
        var bitset = (BitSet) this.bitset.clone();
        return new OffsetBitSet(bitset, this.offset, this.activeBits);
    }
}

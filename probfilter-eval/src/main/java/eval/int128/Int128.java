package eval.int128;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;


public record Int128(long high, long low) implements Comparable<Int128> {
    public Int128() {
        this(0, 0);
    }

    @Override
    public int compareTo(Int128 that) {
        int cmpHi = Long.compareUnsigned(this.high, that.high);
        return cmpHi == 0 ? Long.compareUnsigned(this.low, that.low) : cmpHi;
    }

    public byte[] toBytes() {
        return Bytes.concat(Longs.toByteArray(high), Longs.toByteArray(low));
    }
}

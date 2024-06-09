package probfilter.hash;

import com.google.common.hash.PrimitiveSink;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;


/**
 * A <i>mutable</i> collection that accumulates primitive values for hashing.
 */
@SuppressWarnings("UnstableApiUsage")
public final class Sink {
    private final PrimitiveSink sink;

    Sink(PrimitiveSink sink) {
        this.sink = sink;
    }

    public Sink putByte(byte b) {
        sink.putByte(b);
        return this;
    }

    public Sink putBytes(byte[] bytes) {
        sink.putBytes(bytes);
        return this;
    }

    public Sink putBytes(byte[] bytes, int off, int len) {
        sink.putBytes(bytes, off, len);
        return this;
    }

    public Sink putBytes(ByteBuffer bytes) {
        sink.putBytes(bytes);
        return this;
    }

    public Sink putShort(short s) {
        sink.putShort(s);
        return this;
    }

    public Sink putInt(int i) {
        sink.putInt(i);
        return this;
    }

    public Sink putLong(long l) {
        sink.putLong(l);
        return this;
    }

    public Sink putFloat(float f) {
        sink.putFloat(f);
        return this;
    }

    public Sink putDouble(double d) {
        sink.putDouble(d);
        return this;
    }

    public Sink putBoolean(boolean b) {
        sink.putBoolean(b);
        return this;
    }

    public Sink putChar(char c) {
        sink.putChar(c);
        return this;
    }

    public Sink putUnencodedChars(CharSequence charSequence) {
        sink.putUnencodedChars(charSequence);
        return this;
    }

    public Sink putString(CharSequence charSequence, Charset charset) {
        sink.putString(charSequence, charset);
        return this;
    }
}

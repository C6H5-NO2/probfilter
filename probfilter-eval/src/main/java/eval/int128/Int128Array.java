package eval.int128;

import java.io.*;
import java.util.Arrays;


/**
 * A mutable compact array of {@link Int128}.
 */
public final class Int128Array implements Serializable {
    private long[] array;

    public Int128Array(int length) {
        this.array = new long[length << 1];
    }

    public int length() {
        return array.length >>> 1;
    }

    public Int128 get(int i) {
        long high = array[i << 1];
        long low = array[(i << 1) + 1];
        return new Int128(high, low);
    }

    public void set(int i, Int128 int128) {
        array[i << 1] = int128.high();
        array[(i << 1) + 1] = int128.low();
    }

    @Override
    public String toString() {
        return Arrays.toString(array);
    }

    private void write(DataOutput out) throws IOException {
        int length = this.array.length;
        out.writeInt(length >>> 1);
        for (int i = 0; i < length; ++i)
            out.writeLong(this.array[i]);
    }

    private void read(DataInput in) throws IOException {
        int length = in.readInt() << 1;
        long[] array = new long[length];
        for (int i = 0; i < length; ++i)
            array[i] = in.readLong();
        this.array = array;
    }

    public void writeFile(File file) throws IOException {
        try (var fos = new FileOutputStream(file);
             var bos = new BufferedOutputStream(fos);
             var dos = new DataOutputStream(bos)) {
            write(dos);
        }
    }

    public void readFile(File file) throws IOException {
        try (var fis = new FileInputStream(file);
             var bis = new BufferedInputStream(fis);
             var dis = new DataInputStream(bis)) {
            read(dis);
        }
    }

    @Serial
    private void writeObject(ObjectOutputStream oos) throws IOException {
        write(oos);
    }

    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        read(ois);
    }

    @Serial
    private void readObjectNoData() throws ObjectStreamException {
        this.array = new long[0];
    }
}

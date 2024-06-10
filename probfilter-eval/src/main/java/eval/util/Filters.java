package eval.util;

import com.c6h5no2.probfilter.crdt.CvRFilter;
import com.c6h5no2.probfilter.crdt.FluentCvRFilter;
import eval.akka.AkkaGSet;
import eval.akka.AkkaORSet;
import eval.akka.AkkaSerializer;
import eval.int128.Int128;
import eval.int128.Int128Array;
import scala.Tuple2;

import java.io.*;
import java.util.Arrays;
import java.util.Random;


public final class Filters {
    private Filters() {}

    public static Tuple2<FluentCvRFilter<Int128>, Integer> fill(
        FluentCvRFilter<Int128> filter,
        Int128Array data,
        Slice src
    ) {
        int numSuccess = 0;
        for (int i = src.from(); i < src.until(); ++i) {
            var int128 = data.get(i);
            var result = filter.tryAdd(int128);
            if (result.isSuccess()) {
                filter = result.get();
                ++numSuccess;
            } else {
                break;
            }
        }
        return new Tuple2<>(filter, numSuccess);
    }

    public static final class FalseNegativeException extends RuntimeException {}

    /**
     * @param inserted previously inserted data. Add an additional {@code Slice.fromLength(maxUntil, 0)}
     * for reserving the {@link OffsetBitSet}.
     */
    public static FluentCvRFilter<Int128> drop(
        FluentCvRFilter<Int128> filter,
        Int128Array data,
        int dropCount,
        Random rng,
        Slice... inserted
    ) {
        if (inserted.length == 0) {
            throw new IllegalArgumentException();
        }
        int insertedSize = Arrays.stream(inserted).mapToInt(Slice::length).sum();
        if (dropCount > insertedSize) {
            throw new IllegalArgumentException();
        }
        int minFrom = Arrays.stream(inserted).mapToInt(Slice::from).min().getAsInt();
        int maxUntil = Arrays.stream(inserted).mapToInt(Slice::until).max().getAsInt();
        var containedMask = new OffsetBitSet(maxUntil, minFrom);
        for (var ins : inserted) {
            containedMask.set(ins.from(), ins.until());
        }
        return drop(filter, data, dropCount, rng, containedMask);
    }

    /**
     * @apiNote {@code containedMask} is mutated.
     */
    public static FluentCvRFilter<Int128> drop(
        FluentCvRFilter<Int128> filter,
        Int128Array data,
        int dropCount,
        Random rng,
        OffsetBitSet containedMask
    ) {
        int insertedSize = containedMask.cardinality();
        if (dropCount > insertedSize) {
            throw new IllegalArgumentException();
        }
        for (int dropped = 0; dropped < dropCount; ++dropped) {
            if (insertedSize - dropped < (1 << 12)) {
                return dropSmall(filter, data, dropCount - dropped, rng, containedMask);
            }
            int minFrom = containedMask.offset;
            int maxUntil = containedMask.length();
            int indexToDrop = -1;
            for (int attempts = 0; ; ++attempts) {
                if (attempts > (Integer.MAX_VALUE >> 1)) {
                    // prevent infinite loop
                    throw new RuntimeException();
                }
                indexToDrop = rng.nextInt(minFrom, maxUntil);
                if (containedMask.get(indexToDrop)) {
                    break;
                }
            }
            containedMask.clear(indexToDrop);
            var int128 = data.get(indexToDrop);
            if (!filter.contains(int128)) {
                throw new FalseNegativeException();
            }
            filter = filter.remove(int128);
        }
        return filter;
    }

    /**
     * @apiNote {@code containedMask} is mutated.
     */
    private static FluentCvRFilter<Int128> dropSmall(
        FluentCvRFilter<Int128> filter,
        Int128Array data,
        int dropCount,
        Random rng,
        OffsetBitSet containedMask
    ) {
        var containedArray = containedMask.stream().toArray();
        for (int dropped = 0; dropped < dropCount; ++dropped) {
            int maxUntil = containedArray.length - dropped;
            int indexInArray = rng.nextInt(maxUntil);
            int indexToDrop = containedArray[indexInArray];
            containedArray[indexInArray] = containedArray[maxUntil - 1];
            containedMask.clear(indexToDrop);
            var int128 = data.get(indexToDrop);
            if (!filter.contains(int128)) {
                throw new FalseNegativeException();
            }
            filter = filter.remove(int128);
        }
        return filter;
    }

    public static double measureEmpiricalFpp(FluentCvRFilter<Int128> filter, Int128Array data, Slice tests) {
        int numFp = 0;
        for (int i = tests.from(); i < tests.until(); ++i) {
            var int128 = data.get(i);
            boolean result = filter.contains(int128);
            if (result) {
                ++numFp;
            }
        }
        return (double) numFp / tests.length();
    }

    public static int getSerializedSize(CvRFilter<?, ?> filter) {
        if (filter instanceof AkkaGSet<?> gset) {
            return AkkaSerializer.getInstance().gsetToProto(gset.getAkkaSet()).getSerializedSize();
        } else if (filter instanceof AkkaORSet<?> orset) {
            return AkkaSerializer.getInstance().orsetToProto(orset.getAkkaSet()).getSerializedSize();
        } else {
            try (var bos = new ByteArrayOutputStream();
                 var oos = new ObjectOutputStream(bos)) {
                oos.writeObject(filter);
                oos.flush();
                oos.close();
                return bos.size();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T deepcopy(T obj) {
        T copy = null;
        try {
            var bos = new ByteArrayOutputStream();
            var oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            var bytes = bos.toByteArray();
            var bis = new ByteArrayInputStream(bytes);
            var ois = new ObjectInputStream(bis);
            copy = (T) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return copy;
    }
}

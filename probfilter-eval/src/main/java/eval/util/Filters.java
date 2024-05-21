package eval.util;

import eval.akka.AkkaGSet;
import eval.akka.AkkaORSet;
import eval.akka.AkkaSerializer;
import eval.int128.Int128;
import eval.int128.Int128Array;
import probfilter.crdt.Convergent;
import probfilter.pdsa.Filter;

import java.io.*;
import java.util.Arrays;
import java.util.Random;


public final class Filters {
    private Filters() {}

    public record FillResult(Filter<Int128> filter, int numSuccess) {
        public FillResult merge(FillResult that) {
            return new FillResult(Filters.merge(this.filter, that.filter), this.numSuccess + that.numSuccess);
        }
    }

    public static FillResult fill(Filter<Int128> filter, Int128Array data, Slice src) {
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
        return new FillResult(filter, numSuccess);
    }

    /**
     * @param inserted Add an additional {@code Slice.fromLength(maxUntil, 0)} to reserve.
     */
    public static Filter<Int128> drop(Filter<Int128> filter, Int128Array data, Random rnd, int toDrop, Slice... inserted) {
        if (inserted.length == 0)
            throw new IllegalArgumentException();
        int insertedSize = Arrays.stream(inserted).mapToInt(Slice::length).sum();
        if (toDrop > insertedSize)
            throw new IllegalArgumentException();
        int minFrom = Arrays.stream(inserted).mapToInt(Slice::from).min().getAsInt();
        int maxUntil = Arrays.stream(inserted).mapToInt(Slice::until).max().getAsInt();
        var containedMask = new OffsetBitSet(maxUntil, minFrom);
        for (var ins : inserted) {
            containedMask.set(ins.from(), ins.until());
        }
        return drop(filter, data, rnd, toDrop, containedMask);
    }

    /**
     * @apiNote {@code containedMask} is mutated.
     */
    public static Filter<Int128> drop(Filter<Int128> filter, Int128Array data, Random rnd, int toDrop, OffsetBitSet containedMask) {
        int insertedSize = containedMask.cardinality();
        if (toDrop > insertedSize)
            throw new IllegalArgumentException();
        for (int dropped = 0, attempts = 0; dropped < toDrop; ) {
            if (insertedSize - dropped < (1 << 10)) {
                return dropSmall(filter, data, rnd, toDrop - dropped, containedMask);
            }
            int minFrom = containedMask.offset;
            int maxUntil = containedMask.length();
            int indexToDrop = rnd.nextInt(minFrom, maxUntil);
            if (!containedMask.get(indexToDrop)) {
                ++attempts;
                if (attempts > (Integer.MAX_VALUE >> 1))
                    throw new RuntimeException();
                continue;
            }
            containedMask.clear(indexToDrop);
            var int128 = data.get(indexToDrop);
            if (!filter.contains(int128))
                throw new RuntimeException();
            filter = filter.remove(int128);
            ++dropped;
            attempts = 0;
        }
        return filter;
    }

    /**
     * @apiNote {@code containedMask} is mutated.
     */
    private static Filter<Int128> dropSmall(Filter<Int128> filter, Int128Array data, Random rnd, int toDrop, OffsetBitSet containedMask) {
        // int insertedSize = containedMask.cardinality();
        // if (insertedSize > (1 << 10))
        //     throw new IllegalArgumentException();
        // if (toDrop > insertedSize)
        //     throw new IllegalArgumentException();
        var containedArray = containedMask.stream().toArray();
        if (toDrop > containedArray.length)
            throw new IllegalArgumentException();
        for (int dropped = 0; dropped < toDrop; ++dropped) {
            int maxUntil = containedArray.length - dropped;
            int indexInArray = rnd.nextInt(maxUntil);
            int indexToDrop = containedArray[indexInArray];
            containedArray[indexInArray] = containedArray[maxUntil - 1];
            containedMask.clear(indexToDrop);
            var int128 = data.get(indexToDrop);
            if (!filter.contains(int128))
                throw new RuntimeException();
            filter = filter.remove(int128);
        }
        return filter;
    }

    public static Filter<Int128> merge(Filter<Int128> thiz, Filter<Int128> that) {
        //noinspection unchecked
        return ((Convergent<Filter<Int128>>) thiz).merge(that);
    }

    public static double measureFpp(Filter<Int128> filter, Int128Array data, Slice tests) {
        int numFP = 0;
        for (int i = tests.from(); i < tests.until(); ++i) {
            var int128 = data.get(i);
            boolean result = filter.contains(int128);
            if (result) {
                ++numFP;
            }
        }
        return (double) numFP / tests.length();
    }

    public static int getSerializedSize(Filter<Int128> filter) {
        if (filter instanceof AkkaGSet<Int128> gset) {
            return AkkaSerializer.getInstance().gsetToProto(gset.getUnderlying()).getSerializedSize();
        } else if (filter instanceof AkkaORSet<Int128> orset) {
            return AkkaSerializer.getInstance().orsetToProto(orset.getUnderlying()).getSerializedSize();
        } else {
            try (var bos = new ByteArrayOutputStream();
                 var oos = new ObjectOutputStream(bos)) {
                oos.writeObject(filter);
                oos.flush();
                return bos.size();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Deprecated
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
            //noinspection unchecked
            copy = (T) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return copy;
    }
}

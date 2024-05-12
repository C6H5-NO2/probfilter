package eval.util;

import eval.akka.AkkaGSet;
import eval.akka.AkkaORSet;
import eval.akka.AkkaSerializer;
import eval.int128.Int128;
import eval.int128.Int128Array;
import probfilter.crdt.Convergent;
import probfilter.pdsa.Filter;
import scala.collection.ArrayOps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.BitSet;


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

    public static Filter<Int128> drop(Filter<Int128> filter, Int128Array data, Slice toDrop, Slice... inserted) {
        if (inserted.length == 0)
            throw new IllegalArgumentException();
        //noinspection DataFlowIssue
        int[] cumulativeInsertedLength = (int[]) (Object) ArrayOps.scanLeft$extension(
            scala.Predef.refArrayOps(inserted), 0, (sum, slice) -> (int) sum + ((Slice) slice).length(), scala.reflect.ClassTag.Int()
        );
        //noinspection ConstantValue
        assert cumulativeInsertedLength != null;
        int insertedLength = cumulativeInsertedLength[cumulativeInsertedLength.length - 1];
        if (insertedLength < toDrop.length())
            throw new IllegalArgumentException();
        var droppedMask = new BitSet(insertedLength);
        for (int i = toDrop.from(); i < toDrop.until(); ++i) {
            int indexToDrop = (int) (data.get(i).low() & Integer.MAX_VALUE) % insertedLength;
            indexToDrop = droppedMask.nextClearBit(indexToDrop);
            if (indexToDrop >= insertedLength) {
                indexToDrop = droppedMask.nextClearBit(0);
            }
            assert indexToDrop < insertedLength;
            final int finalIndexToDrop = indexToDrop;
            droppedMask.set(finalIndexToDrop);
            int sliceIndex = ArrayOps.lastIndexWhere$extension(
                scala.Predef.intArrayOps(cumulativeInsertedLength), (sum) -> (int) sum <= finalIndexToDrop, Integer.MAX_VALUE
            );
            int dataIndex = inserted[sliceIndex].from() + (finalIndexToDrop - cumulativeInsertedLength[sliceIndex]);
            var int128 = data.get(dataIndex);
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
}

package probfilter.pdsa;

import scala.Tuple2;

import java.util.stream.LongStream;


/**
 * An immutable view of a bucket in {@link probfilter.pdsa.LongCuckooTable}.
 */
public interface LongCuckooBucket {
    /**
     * @return number of (distinct) fingerprints in this bucket
     */
    int size();

    /**
     * @return {@code true} if this bucket contains any entries with fingerprint {@code fp}
     */
    boolean contains(byte fp);

    /**
     * @return a copy of entries with fingerprint {@code fp} in this bucket
     */
    long[] get(byte fp);

    /**
     * @return a stream of entries in this bucket
     */
    LongStream stream();

    /**
     * @return an array of entries in both buckets
     */
    default long[] concat(LongCuckooBucket that) {
        return LongStream.concat(this.stream(), that.stream()).toArray();
    }

    /**
     * @return a table with entry {@code long$} added to this bucket
     */
    LongCuckooTable add(long long$);

    /**
     * @return a table with entries {@code longs} added to this bucket
     */
    LongCuckooTable add(long[] longs);

    /**
     * @return a table with entries having fingerprint {@code fp} in this bucket removed
     */
    LongCuckooTable remove(byte fp);

    /**
     * Replaces entries with a same random fingerprint in this bucket by {@code longs}.
     *
     * @return a tuple of the new table and the replaced entries
     * @throws java.lang.IllegalArgumentException if {@code longs} is empty
     * @throws java.util.NoSuchElementException if this bucket is empty
     */
    Tuple2<LongCuckooTable, long[]> replace(long[] longs);
}

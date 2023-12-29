package probfilter.pdsa;

import com.google.common.primitives.Bytes;
import probfilter.util.JavaFriendly;
import probfilter.util.Pair;
import scala.Tuple2;


/**
 * An immutable view of a bucket in {@link probfilter.pdsa.ByteCuckooTable}.
 */
public interface ByteCuckooBucket {
    /**
     * @return number of (distinct) fingerprints in this bucket
     */
    int size();

    /**
     * @return {@code true} if this bucket contains the fingerprint {@code fp}
     */
    boolean contains(byte fp);

    /**
     * @return a copy of fingerprints in this bucket
     */
    byte[] get();

    /**
     * @return an array of fingerprints in both buckets
     * @apiNote {@code distinct} not called internally
     */
    default byte[] concat(ByteCuckooBucket that) {
        return Bytes.concat(this.get(), that.get());
    }

    /**
     * @return a table with fingerprint {@code byte$} added to this bucket
     */
    ByteCuckooTable add(byte byte$);

    /**
     * Replaces a random fingerprint in this bucket by fingerprint {@code byte$}.
     *
     * @return a tuple of the new table and the replaced fingerprint
     * @throws java.util.NoSuchElementException if this bucket is empty
     */
    Tuple2<ByteCuckooTable, scala.Byte> replace(byte byte$);

    @JavaFriendly(scalaDelegate = "probfilter.pdsa.ByteCuckooBucket::replace")
    default Pair<ByteCuckooTable, Byte> replaceAsJava(byte byte$) {
        var tuple = replace(byte$);
        return new Pair<>(tuple._1, tuple._2.toByte());
    }
}

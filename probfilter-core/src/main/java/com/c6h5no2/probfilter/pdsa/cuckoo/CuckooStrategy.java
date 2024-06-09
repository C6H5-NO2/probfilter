package probfilter.pdsa.cuckoo;

import probfilter.pdsa.FilterHashStrategy;


/**
 * @see probfilter.pdsa.cuckoo.SimpleCuckooStrategy
 */
public interface CuckooStrategy<E> extends FilterHashStrategy {
    @Override
    default double fpp() {
        return 2.0 * bucketSize() / (1 << fingerprintBits());
    }

    @Override
    CuckooStrategy<E> tighten();

    int numBuckets();

    int bucketSize();

    int maxIterations();

    int fingerprintBits();

    EntryStorageType storageType();

    /**
     * @return {@code 0 <= i < numBuckets}
     */
    int indexHash(E elem);

    /**
     * @return {@code 1 <= fp <= (... 1111 1111)_2}
     */
    short fingerprintHash(E elem);

    /**
     * Any involution suffices.
     */
    int altIndexOf(int i, short fp);

    /**
     * @return {@code i = indexHash(elem); fp = fingerprintHash(elem)}
     */
    default Pair hash(E elem) {
        var i = indexHash(elem);
        var f = fingerprintHash(elem);
        return new Pair(i, f);
    }

    default Triple hashAll(E elem) {
        var i = indexHash(elem);
        var f = fingerprintHash(elem);
        var j = altIndexOf(i, f);
        return new Triple(i, j, f);
    }

    record Pair(int i, short fp) {}

    record Triple(int i, int j, short fp) {}

    class MaxIterationReachedException extends RuntimeException {
        public MaxIterationReachedException(Object elem, int max) {
            super("Reached maximum number of iterations of " + max + " when adding " + elem);
        }
    }

    final class FingerprintLengthExceededException extends RuntimeException {
        public FingerprintLengthExceededException() {
            super("Exceeded current fingerprint length limit of 16 bits");
        }
    }
}

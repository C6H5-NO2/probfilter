package com.c6h5no2.probfilter.pdsa.cuckoo;

import com.c6h5no2.probfilter.pdsa.HashStrategy;


public interface CuckooStrategy<E> extends HashStrategy {
    @Override
    default double fpp() {
        return 2.0 * bucketSize() / (1 << fingerprintBits());
    }

    @Override
    CuckooStrategy<E> tighten();

    /**
     * @return number of buckets in the cuckoo table
     */
    int numBuckets();

    /**
     * @return expected number of slots per bucket
     */
    int bucketSize();

    /**
     * @return quota for cuckoo eviction
     */
    int maxIterations();

    /**
     * @return length of fingerprint in bits
     */
    int fingerprintBits();

    /**
     * @return semantic type of entry
     */
    CuckooEntryType entryType();

    /**
     * @return {@code 0 <= i < numBuckets}
     */
    int indexHash(E elem);

    /**
     * @return {@code 1 <= fp <= (... 1111 1111)_2}
     */
    short fingerprintHash(E elem);

    /**
     * @implNote Any involution (given {@code fp}) suffices.
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

    final class MaxIterationReachedException extends RuntimeException {
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

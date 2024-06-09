package com.c6h5no2.probfilter.util;

import java.io.Serializable;


/**
 * A mutable random number generator.
 */
public interface RandomIntGenerator extends Mutable, Serializable {
    /**
     * @return a non-negative random integer
     */
    int nextInt();

    /**
     * @param bound a positive integer
     * @return a non-negative random integer within {@code [0, bound)}
     */
    default int nextInt(int bound) {
        return nextInt() % bound;
    }

    /**
     * @return a <i>new instance</i> of {@link RandomIntGenerator} with the same state
     * @implNote This operation should be cheap.
     */
    RandomIntGenerator copy();
}

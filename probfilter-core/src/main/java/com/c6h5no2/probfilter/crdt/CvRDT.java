package com.c6h5no2.probfilter.crdt;

import java.io.Serializable;


/**
 * A convergent replicated data type (CvRDT).
 *
 * @param <T> the type that forms the join semilattice
 */
public interface CvRDT<T> extends Serializable {
    /**
     * @return {@code true} if {@code this} is partially less than or equal to {@code that} in the join semilattice
     * @apiNote This operation may not be implemented.
     * @implNote It is not mandatory to fully implement this function, as the function is not used.
     */
    default boolean lteq(T that) {
        throw new RuntimeException(new scala.NotImplementedError(getClass().getName() + "#lteq"));
    }

    /**
     * @deprecated The naming is not very clear; use {@link CvRDT#lteq(T)} instead.
     */
    @Deprecated
    default boolean compare(T that) {
        return lteq(that);
    }

    /**
     * @return a new instance of {@code T} that is the least upper bound of {@code this} and {@code that}
     */
    T merge(T that);
}

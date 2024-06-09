package com.c6h5no2.probfilter.hash;

import java.io.Serializable;


/**
 * A funnel that unpacks an object as primitive values.
 *
 * @see Funnels$ Funnels
 */
@FunctionalInterface
public interface Funnel<T> extends Serializable {
    /**
     * Unpacks {@code T from} as primitives into {@link Sink} {@code into}.
     */
    void apply(T from, Sink into);
}

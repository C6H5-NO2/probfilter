package com.c6h5no2.probfilter.pdsa;

import com.c6h5no2.probfilter.util.Immutable;

import java.io.Serializable;


/**
 * An immutable class to hold functional hash logics for {@link Filter}s.
 */
public interface HashStrategy extends Immutable, Serializable {
    int capacity();

    double fpp();

    /**
     * @return a new instance of {@link HashStrategy} with fpp half that of {@code this}
     * @throws java.lang.RuntimeException if the strategy cannot be further tightened due to practical limitations
     * @apiNote {@link HashStrategy#capacity} may also be increased.
     */
    HashStrategy tighten();
}

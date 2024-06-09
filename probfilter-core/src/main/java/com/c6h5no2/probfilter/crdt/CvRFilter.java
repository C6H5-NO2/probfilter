package com.c6h5no2.probfilter.crdt;

import com.c6h5no2.probfilter.pdsa.Filter;


/**
 * A convergent (i.e. state-based) replicated filter.
 */
public interface CvRFilter<E, T> extends Filter<E, T>, CvRDT<T> {
    default FluentCvRFilter<E> asFluent() {
        return new FluentCvRFilter<>(this);
    }
}

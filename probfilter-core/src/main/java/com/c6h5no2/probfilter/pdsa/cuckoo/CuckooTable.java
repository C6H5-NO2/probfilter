package com.c6h5no2.probfilter.pdsa.cuckoo;

import java.io.Serializable;


/**
 * A cuckoo hash table for cuckoo filters.
 *
 * @see TypedCuckooTable
 */
public sealed interface CuckooTable extends Serializable permits TypedCuckooTable {
    @SuppressWarnings("unchecked")
    default <T> TypedCuckooTable<T> typed() {
        return (TypedCuckooTable<T>) this;
    }
}

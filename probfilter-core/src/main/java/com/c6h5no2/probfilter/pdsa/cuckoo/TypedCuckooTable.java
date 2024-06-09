package com.c6h5no2.probfilter.pdsa.cuckoo;


/**
 * A generic {@link CuckooTable} to please the compiler.
 *
 * @param <T> the primitive type that the entries are stored in
 * @see MapCuckooTable
 * @see ArrayCuckooTable
 */
public non-sealed interface TypedCuckooTable<T> extends CuckooTable, TypedCuckooTableOps<T> {}

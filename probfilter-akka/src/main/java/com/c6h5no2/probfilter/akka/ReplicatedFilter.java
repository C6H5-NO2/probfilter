package com.c6h5no2.probfilter.akka;

import akka.cluster.ddata.ReplicatedData;
import com.c6h5no2.probfilter.crdt.CvRFilter;
import com.c6h5no2.probfilter.util.Immutable;
import com.google.common.base.MoreObjects;

import java.io.Serial;


/**
 * A type-erased adapter for {@link Immutable} {@link CvRFilter} implementing Akka's
 * {@link akka.cluster.ddata.ReplicatedData ReplicatedData}.
 *
 * @see ReplicatedFilterKey
 */
public final class ReplicatedFilter implements ReplicatedData, CvRFilter<Object, ReplicatedData> {
    @Serial
    private static final long serialVersionUID = 1L;

    private final CvRFilter<Object, CvRFilter<Object, ?>> filter;

    @SuppressWarnings("unchecked")
    private ReplicatedFilter(CvRFilter<?, ?> filter) {
        this.filter = (CvRFilter<Object, CvRFilter<Object, ?>>) filter;
    }

    public static <T extends CvRFilter<?, ? super T> & Immutable> ReplicatedFilter apply(T filter) {
        return new ReplicatedFilter(filter);
    }

    @Override
    public int size() {
        return filter.size();
    }

    @Override
    public int capacity() {
        return filter.capacity();
    }

    @Override
    public double fpp() {
        return filter.fpp();
    }

    @Override
    public boolean contains(Object elem) {
        return filter.contains(elem);
    }

    @Override
    public ReplicatedFilter add(Object elem) {
        return copy(filter.add(elem));
    }

    @Override
    public ReplicatedFilter remove(Object elem) {
        return copy(filter.remove(elem));
    }

    @Override
    public boolean lteq(ReplicatedData that) {
        if (that instanceof ReplicatedFilter das) {
            return this.filter.lteq(das.filter);
        } else {
            throw new UnsupportedOperationException(getClass().getName() + "#lteq");
        }
    }

    @Override
    public ReplicatedFilter merge(ReplicatedData that) {
        if (that instanceof ReplicatedFilter das) {
            return copy(this.filter.merge(das.filter));
        } else {
            throw new UnsupportedOperationException(getClass().getName() + "#merge");
        }
    }

    private ReplicatedFilter copy(CvRFilter<?, ?> filter) {
        return new ReplicatedFilter(filter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("filter", filter).toString();
    }
}

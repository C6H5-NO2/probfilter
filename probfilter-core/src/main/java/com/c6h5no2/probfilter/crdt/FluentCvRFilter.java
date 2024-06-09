package com.c6h5no2.probfilter.crdt;

import com.google.common.base.MoreObjects;
import scala.util.Failure;
import scala.util.Try;

import java.io.Serial;


/**
 * A CvRFilter with fluent interface.
 */
public final class FluentCvRFilter<E> implements CvRFilter<E, FluentCvRFilter<E>> {
    @Serial
    private static final long serialVersionUID = 1L;

    private final CvRFilter<E, CvRFilter<E, ?>> filter;

    @SuppressWarnings("unchecked")
    FluentCvRFilter(CvRFilter<?, ?> filter) {
        this.filter = (CvRFilter<E, CvRFilter<E, ?>>) filter;
    }

    public static <A, T extends CvRFilter<?, T>> FluentCvRFilter<A> apply(CvRFilter<? super A, T> filter) {
        return new FluentCvRFilter<>(filter);
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
    public boolean contains(E elem) {
        return filter.contains(elem);
    }

    @Override
    public FluentCvRFilter<E> add(E elem) {
        return copy(filter.add(elem));
    }

    @Override
    public Try<FluentCvRFilter<E>> tryAdd(E elem) {
        var tryCopied = filter.tryAdd(elem).map(this::copy);
        // i.e. `tryCopied match { case failure@Failure(_: ClassCastException) => ... }`
        if (tryCopied instanceof Failure<?> failure && failure.exception() instanceof ClassCastException) {
            // throw the exception
            failure.get();
        }
        return tryCopied;
    }

    @Override
    public FluentCvRFilter<E> remove(E elem) {
        return copy(filter.remove(elem));
    }

    @Override
    public boolean lteq(FluentCvRFilter<E> that) {
        return this.filter.lteq(that.filter);
    }

    @Override
    public FluentCvRFilter<E> merge(FluentCvRFilter<E> that) {
        return copy(this.filter.merge(that.filter));
    }

    private FluentCvRFilter<E> copy(CvRFilter<E, ?> filter) {
        return new FluentCvRFilter<>(filter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("filter", filter).toString();
    }
}

package com.c6h5no2.probfilter.pdsa;

import scala.util.Failure;
import scala.util.Try;

import java.io.Serializable;


/**
 * A probabilistic filter.
 *
 * @param <E> type of element
 * @param <T> F-bounded type of implementing subtype
 */
public interface Filter<E, T> extends Serializable {
    /**
     * @return a <i>possibly estimated</i> number of elements in this filter
     */
    int size();

    /**
     * @return expected number of elements this filter can hold before performance deteriorates
     */
    int capacity();

    /**
     * @return false positive probability
     */
    double fpp();

    /**
     * @return {@code true} if this filter <i>might</i> contain {@code elem}; {@code false} when definitely not
     */
    boolean contains(E elem);

    /**
     * @return a filter with {@code elem} added
     * @throws java.lang.RuntimeException if failed due to the properties of this filter
     * @see Filter#tryAdd(E)
     */
    T add(E elem);

    /**
     * @throws java.lang.RuntimeException if that exception is not caught
     * @apiNote This function (is expected to) catch only exceptions due to the properties of this filter.
     * @implNote The default implementation catches all {@link scala.util.control.NonFatal$ NonFatal} exceptions
     * except for {@link java.lang.ClassCastException} and {@link scala.MatchError}; override if necessary.
     * @see Filter#add(E)
     */
    default Try<T> tryAdd(E elem) {
        var tryAdded = Try.apply(() -> add(elem));
        // i.e. `tryAdded match { case failure@Failure(_: ClassCastException | _: MatchError) => failure.get }`
        if (tryAdded instanceof Failure<?> failure) {
            var exception = failure.exception();
            if (exception instanceof ClassCastException || exception instanceof scala.MatchError) {
                // throw the exception
                failure.get();
            }
        }
        return tryAdded;
    }

    /**
     * @return a filter with {@code elem} removed
     * @throws java.lang.UnsupportedOperationException if the operation is not supported
     * @apiNote No exception will be thrown if {@code elem} does not exist in the first place.
     */
    default T remove(E elem) {
        throw new UnsupportedOperationException(getClass().getName() + "#remove");
    }
}

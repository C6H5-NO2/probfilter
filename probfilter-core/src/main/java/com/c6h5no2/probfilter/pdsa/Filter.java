package probfilter.pdsa;

import scala.util.Try;

import java.io.Serializable;


/**
 * Base interface of probabilistic filters.
 */
public interface Filter<E> extends Serializable {
    /**
     * @return a <i>possibly estimated</i> number of elements in this filter
     */
    int size();

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
     * @throws java.lang.RuntimeException if failed due to the properties of the filter
     * @see probfilter.pdsa.Filter#tryAdd(E)
     */
    Filter<E> add(E elem);

    /**
     * @implNote Any unchecked exception should be wrapped in the return value.
     * @see probfilter.pdsa.Filter#add(E)
     */
    default Try<Filter<E>> tryAdd(E elem) {
        return Try.apply(() -> add(elem));
    }

    /**
     * @return a filter with {@code elem} removed
     * @throws java.lang.UnsupportedOperationException if not supported
     */
    default Filter<E> remove(E elem) {
        throw new UnsupportedOperationException(getClass().getName() + "#remove");
    }
}

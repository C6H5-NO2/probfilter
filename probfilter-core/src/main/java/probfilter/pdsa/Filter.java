package probfilter.pdsa;

import scala.util.Try;

import java.io.Serializable;


/**
 * Base interface of probabilistic filters.
 */
public interface Filter<E> extends Serializable {
    /**
     * @return number of elements in this filter
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
     * @return a new instance of {@code T} with {@code elem} added
     * @throws java.lang.RuntimeException if failed due to the properties of the filter
     * @see probfilter.pdsa.Filter#tryAdd(E)
     */
    Filter<E> add(E elem);

    default Try<Filter<E>> tryAdd(E elem) {
        return Try.apply(() -> add(elem));
    }

    /**
     * @return a new instance of {@code T} with {@code elem} removed
     * @throws java.lang.UnsupportedOperationException if not supported
     */
    default Filter<E> remove(E elem) {
        throw new UnsupportedOperationException(getClass().getName() + "#remove");
    }
}

package probfilter.crdt;

import scala.util.Try;


public interface BaseFilter<E, T> extends Mergeable<T> {
    int size();

    /**
     * @return {@code true} if this filter <i>might</i> contain {@code elem}; {@code false} when definitely not
     */
    boolean contains(E elem);

    /**
     * @return a new instance of {@code T} with {@code elem} added
     * @throws RuntimeException if failed due to the properties of this filter
     * @see probfilter.crdt.BaseFilter#tryAdd(E)
     */
    T add(E elem);

    default Try<T> tryAdd(E elem) {
        return Try.apply(() -> add(elem));
    }

    /**
     * @return a new instance of {@code T} with {@code elem} removed
     * @throws UnsupportedOperationException if not supported
     */
    default T remove(E elem) {
        throw new UnsupportedOperationException(getClass().getName() + "#remove");
    }
}

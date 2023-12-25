package probfilter.crdt;

import probfilter.util.Mergeable;
import scala.util.Try;

import java.io.Serializable;


public interface BaseFilter<E, T extends BaseFilter<E, T> & Mergeable<T>> extends Serializable {
    /**
     * @return {@code true} if {@code elem} might be contained in the filter; {@code false} when definitely not
     */
    boolean mightContains(E elem);

    /**
     * @throws RuntimeException if failed due to the properties of the filter
     */
    T add(E elem);

    default Try<T> tryAdd(E elem) {
        return Try.apply(() -> add(elem));
    }

    /**
     * @throws UnsupportedOperationException if not supported
     */
    default T remove(E elem) {
        throw new UnsupportedOperationException(getClass().getName() + ".remove");
    }
}

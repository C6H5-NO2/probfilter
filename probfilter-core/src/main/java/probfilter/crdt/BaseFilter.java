package probfilter.crdt;

import scala.util.Try;

import java.io.Serializable;


public abstract class BaseFilter<E, T extends BaseFilter<E, T>> implements Mergeable<T>, Serializable {
    /**
     * @return {@code true} if {@code elem} might be contained in the filter; {@code false} when definitely not
     */
    public abstract boolean mightContains(E elem);

    /**
     * @throws RuntimeException if failed due to the properties of the filter
     */
    public abstract T add(E elem);

    public final Try<T> tryAdd(E elem) {
        return Try.apply(() -> add(elem));
    }

    /**
     * @throws UnsupportedOperationException if not supported
     */
    public T remove(E elem) {
        throw new UnsupportedOperationException(getClass().getName() + ".remove");
    }
}

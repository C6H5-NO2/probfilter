package probfilter.crdt;

import java.io.Serializable;


public interface Mergeable<T> extends Serializable {
    /**
     * @return {@code true} if {@code this} is partially less than or equal to {@code that} in the join semi-lattice
     */
    boolean lteq(T that);

    /**
     * @return a new instance of {@code T} that is the mergence of {@code this} and {@code that}
     */
    T merge(T that);
}

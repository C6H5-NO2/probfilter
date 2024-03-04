package probfilter.crdt;

import java.io.Serializable;


public interface Convergent<T> extends Serializable {
    /**
     * @return {@code true} if {@code this} is partially less than or equal to {@code that} in the join semilattice
     */
    boolean lteq(T that);

    /**
     * @return a new instance of {@code T} that is the least upper bound of {@code this} and {@code that}
     */
    T merge(T that);
}

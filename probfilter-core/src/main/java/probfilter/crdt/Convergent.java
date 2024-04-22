package probfilter.crdt;

import java.io.Serializable;


/**
 * A convergent replicated data type (CvRDT).
 *
 * @param <T> the type that can form the join semilattice
 */
public interface Convergent<T> extends Serializable {
    /**
     * @return {@code true} if {@code this} is partially less than or equal to {@code that} in the join semilattice
     * @apiNote It is not mandatory to fully implement this function.
     */
    boolean lteq(T that);

    /**
     * @deprecated The naming is not very clear; use {@link probfilter.crdt.Convergent#lteq(T)} instead.
     */
    @Deprecated
    default boolean compare(T that) {
        return lteq(that);
    }

    /**
     * @return a new instance of {@code T} that is the least upper bound of {@code this} and {@code that}
     */
    T merge(T that);
}

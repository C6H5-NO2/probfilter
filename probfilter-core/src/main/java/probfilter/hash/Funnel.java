package probfilter.hash;

import java.io.Serializable;


/**
 * @see probfilter.hash.Funnels$ probfilter.hash.Funnels
 */
@FunctionalInterface
public interface Funnel<T> extends Serializable {
    /**
     * Unpacks {@code from} as primitives in {@code into}.
     */
    void funnel(T from, Sink into);
}

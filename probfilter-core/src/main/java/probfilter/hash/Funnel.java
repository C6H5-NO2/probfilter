package probfilter.hash;

import java.io.Serializable;


/**
 * A function that accumulates data in an object as primitives in {@code Sink}.
 *
 * @see probfilter.hash.Funnels$ probfilter.hash.Funnels
 */
@FunctionalInterface
public interface Funnel<T> extends Serializable {
    void funnel(T from, Sink into);
}

package probfilter.crdt;

import probfilter.pdsa.Filter;


/**
 * Convenient base interface for convergent replicated filters.
 *
 * @param <E> type of elements
 * @param <T> type of concrete implementation
 */
public interface CvFilter<E, T> extends Filter<E>, Convergent<T> {}

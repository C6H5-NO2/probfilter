package probfilter.crdt.mutable;

import probfilter.crdt.CvFilter;


/**
 * Convenient base interface for <i>mutable</i> convergent replicated filters.
 *
 * @param <E> type of elements
 * @param <T> type of concrete implementation
 */
public interface MutCvFilter<E, T> extends CvFilter<E, T> {}

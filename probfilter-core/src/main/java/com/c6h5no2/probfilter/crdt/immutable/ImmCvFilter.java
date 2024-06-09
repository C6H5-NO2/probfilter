package probfilter.crdt.immutable;

import probfilter.crdt.CvFilter;


/**
 * Convenient base interface for <i>immutable</i> convergent replicated filters.
 *
 * @param <E> type of elements
 * @param <T> type of concrete implementation
 */
public interface ImmCvFilter<E, T> extends CvFilter<E, T> {}

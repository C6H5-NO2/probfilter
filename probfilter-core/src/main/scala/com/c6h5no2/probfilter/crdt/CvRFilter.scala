package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.pdsa.Filter

/**
 * A convergent (i.e. state-based) replicated probabilistic filter.
 */
trait CvRFilter[E, T] extends Filter[E, T] with CvRDT[T] {
  @deprecated
  def asFluent(): FluentCvRFilter[E] = {
    new FluentCvRFilter[E](this)
  }
}

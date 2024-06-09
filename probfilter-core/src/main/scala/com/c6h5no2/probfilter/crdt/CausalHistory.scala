package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.util.UnsignedNumber


trait CausalHistory {
  /**
   * @param replicaId 16-bit unsigned replica id
   * @return 32-bit unsigned timestamp
   */
  def get(replicaId: Short): Int

  /** @note This is a query operation. */
  def next(replicaId: Short): Int = get(replicaId) + 1

  /** @note The default implementation assumes consecutive timestamps; override if necessary. */
  def observes(replicaId: Short, timestamp: Int): Boolean = UnsignedNumber.compare(timestamp, get(replicaId)) <= 0

  def increase(replicaId: Short): CausalHistory
}

package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.pdsa.cuckoo.CuckooStrategy
import com.c6h5no2.probfilter.util.ClassEx.Clazz
import com.c6h5no2.probfilter.util.{RandomIntGenerator, SimpleLCG, Immutable => ImmCol, Mutable => MutCol}

import scala.annotation.tailrec
import scala.util.{Failure, Success}


/** A scalable observed-remove replicated cuckoo filter. */
sealed trait ScORCuckooFilter[E] extends CvRFilter[E, ScORCuckooFilter[E]] {
  override final def size: Int = state.size

  override final def capacity: Int = state.capacity

  override final def contains(elem: E): Boolean = state.contains(elem)

  @tailrec
  override final def add(elem: E): ScORCuckooFilter[E] = {
    val expanded = state.expand()
    val res =
      expanded
        .reverseIterator
        .zipWithIndex
        .map { tup =>
          val sf = tup._1
          val tried = if (sf.size < sf.capacity) sf.tryAdd(elem) else Failure.apply(new RuntimeException())
          (tried, tup._2)
        }
        .find { tup =>
          val tried = tup._1
          tried.isSuccess
        }
    res match {
      case Some((Success(sf), reverseIndex)) =>
        val index = expanded.length - 1 - reverseIndex
        val updated = expanded.set(index, sf)
        val synced = updated.map(_.copy(sf.hist))
        copy(synced)

      case _ => copy(expanded.expand(true)).add(elem)
    }
  }

  override final def remove(elem: E): ScORCuckooFilter[E] = {
    val indexes = state.reverseIterator.zipWithIndex.filter(_._1.contains(elem)).map(_._2).toArray
    if (indexes.isEmpty) {
      this
    } else {
      val rng = rngCopy
      val index = indexes.apply(rng.nextInt(indexes.length))
      val updated = state.update(index, _.remove(elem))
      val hist = updated.head.hist
      copy(updated.shrink(), hist, rng)
    }
  }

  override final def merge(that: ScORCuckooFilter[E]): ScORCuckooFilter[E] = {
    val newState = this.state.merge(that.state)
    val synced = if (newState.length == 0) newState else newState.map(_.copy(newState.head.hist))
    copy(synced)
  }

  protected def state: FilterSeries[E, ORCuckooFilter[E]]

  protected def rngCopy: RandomIntGenerator

  protected def copy(state: FilterSeries[E, ORCuckooFilter[E]]): ScORCuckooFilter[E]

  protected def copy(
    state: FilterSeries[E, ORCuckooFilter[E]],
    hist: VersionVector,
    rng: RandomIntGenerator,
  ): ScORCuckooFilter[E]

  override def toString: String = s"${getClass.getShortName}($state)"
}

object ScORCuckooFilter {
  /**
   * @param initStrategy a strategy whose `fpp` is the expected compounded value
   * @note The first sub-filter is created with `strategy.tighten()`;
   *       note the initial `capacity` and `fingerprintBits`.
   */
  def apply[E](mutable: Boolean, initStrategy: CuckooStrategy[E], rid: Short, seed: Int): ScORCuckooFilter[E] = {
    if (mutable)
      new ScORCuckooFilter.Mutable[E](initStrategy, rid, seed)
    else
      new ScORCuckooFilter.Immutable[E](initStrategy, rid, seed)
  }

  @SerialVersionUID(1L)
  final class Immutable[E] private(
    protected val state: FilterSeries[E, ORCuckooFilter[E]],
    supplier: SubFilterSupplier[E],
    initStrategy: CuckooStrategy[E],
    rng: RandomIntGenerator,
  ) extends ScORCuckooFilter[E]
    with ImmCol {
    private def this(supplier: SubFilterSupplier[E], initStrategy: CuckooStrategy[E], rng: RandomIntGenerator) = {
      this(new FilterSeries[E, ORCuckooFilter[E]](supplier), supplier, initStrategy, rng)
    }

    def this(initStrategy: CuckooStrategy[E], rid: Short, seed: Int) = {
      this(
        new SubFilterSupplier[E](
          new VersionVector(),
          initStrategy,
          rid,
          seed,
          new ORCuckooFilter.Immutable[E](_, _, _)
        ),
        initStrategy,
        new SimpleLCG(seed)
      )
    }

    override def fpp: Double = initStrategy.fpp

    override protected def rngCopy: RandomIntGenerator = rng.copy()

    override protected def copy(state: FilterSeries[E, ORCuckooFilter[E]]): ScORCuckooFilter[E] = {
      val newSupplier = if (state.length == 0) this.supplier else this.supplier.copy(state.head.hist)
      val newState = state.copy(newSupplier)
      new ScORCuckooFilter.Immutable[E](newState, newSupplier, this.initStrategy, this.rng)
    }

    override protected def copy(
      state: FilterSeries[E, ORCuckooFilter[E]],
      hist: VersionVector,
      rng: RandomIntGenerator
    ): ScORCuckooFilter[E] = {
      val newSupplier = this.supplier.copy(hist)
      val newState = state.copy(newSupplier)
      new ScORCuckooFilter.Immutable[E](newState, newSupplier, this.initStrategy, rng.copy())
    }
  }

  @SerialVersionUID(1L)
  final class Mutable[E] private(
    protected var state: FilterSeries[E, ORCuckooFilter[E]],
    private[this] var supplier: SubFilterSupplier[E],
    initStrategy: CuckooStrategy[E],
    private[this] var rng: RandomIntGenerator,
  ) extends ScORCuckooFilter[E]
    with MutCol {
    private def this(supplier: SubFilterSupplier[E], initStrategy: CuckooStrategy[E], rng: RandomIntGenerator) = {
      this(new FilterSeries[E, ORCuckooFilter[E]](supplier), supplier, initStrategy, rng)
    }

    def this(initStrategy: CuckooStrategy[E], rid: Short, seed: Int) = {
      this(
        new SubFilterSupplier[E](
          new VersionVector(),
          initStrategy,
          rid,
          seed,
          new ORCuckooFilter.Mutable[E](_, _, _)
        ),
        initStrategy,
        new SimpleLCG(seed)
      )
    }

    override def fpp: Double = initStrategy.fpp

    override protected def rngCopy: RandomIntGenerator = rng.copy()

    override protected def copy(state: FilterSeries[E, ORCuckooFilter[E]]): ScORCuckooFilter[E] = {
      this.supplier = if (state.length == 0) this.supplier else this.supplier.copy(state.head.hist)
      this.state = state.copy(this.supplier)
      this
    }

    override protected def copy(
      state: FilterSeries[E, ORCuckooFilter[E]],
      hist: VersionVector,
      rng: RandomIntGenerator
    ): ScORCuckooFilter[E] = {
      this.supplier = this.supplier.copy(hist)
      this.state = state.copy(this.supplier)
      this.rng = rng
      this
    }
  }

  @SerialVersionUID(1L)
  private final class SubFilterSupplier[E](
    hist: VersionVector,
    initStrategy: CuckooStrategy[E],
    rid: Short,
    seed: Int,
    constructor: (CuckooStrategy[E], Short, Int) => ORCuckooFilter[E],
  ) extends (Option[ORCuckooFilter[E]] => ORCuckooFilter[E])
    with Serializable {
    override def apply(last: Option[ORCuckooFilter[E]]): ORCuckooFilter[E] =
      constructor.apply(last.fold(initStrategy)(_.strategy).tighten(), rid, seed).copy(hist)

    def copy(hist: VersionVector): SubFilterSupplier[E] =
      new SubFilterSupplier[E](hist, this.initStrategy, this.rid, this.seed, this.constructor)
  }
}

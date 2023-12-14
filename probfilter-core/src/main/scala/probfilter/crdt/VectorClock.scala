package probfilter.crdt

import akka.cluster.{VectorClock => AkkaVectorClock}
import probfilter.crdt.VectorClock.create


@SerialVersionUID(1L)
class VectorClock private(private val clock: AkkaVectorClock) extends Serializable {
  def this() = this(create())

  def get(replicaId: Long): Long = clock.versions.getOrElse(replicaId.toString, 0)

  def inc(replicaId: Long): VectorClock = new VectorClock(clock :+ replicaId.toString)

  def merge(that: VectorClock): VectorClock = new VectorClock(this.clock merge that.clock)

  override def toString: String = clock.toString()
}


object VectorClock {

  import scala.collection.immutable.TreeMap
  import scala.reflect.runtime.universe

  private val ctorMirror: universe.MethodMirror = {
    val rtMirror = universe.runtimeMirror(getClass.getClassLoader)
    val clsSymbol = universe.typeOf[AkkaVectorClock].typeSymbol.asClass
    val clsMirror = rtMirror.reflectClass(clsSymbol)
    val ctorSymbol = clsSymbol.typeSignature.decl(universe.termNames.CONSTRUCTOR).asMethod
    clsMirror.reflectConstructor(ctorSymbol)
  }

  private def create(): AkkaVectorClock = ctorMirror(TreeMap.empty[String, Long]).asInstanceOf[AkkaVectorClock]
}

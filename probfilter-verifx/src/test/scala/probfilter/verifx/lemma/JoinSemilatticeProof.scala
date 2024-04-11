package probfilter.verifx.lemma

import org.scalatest.flatspec.AnyFlatSpecLike
import probfilter.verifx.prover.Prover


trait JoinSemilatticeProof extends AnyFlatSpecLike {
  protected val name: String
  protected val prover: Prover
}

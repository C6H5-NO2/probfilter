package probfilter.verifx

import probfilter.verifx.lemma.CvRDTProof
import probfilter.verifx.prover.Prover


class ORCuckooFilterProof extends CvRDTProof {
  override protected val name: String = "ORCuckooFilter"
  override protected val prover: Prover = new Prover()
  override protected val updateOps: Seq[String] = Seq.apply("add", "remove")
}

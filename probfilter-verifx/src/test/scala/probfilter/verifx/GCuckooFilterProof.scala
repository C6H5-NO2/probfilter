package probfilter.verifx

import probfilter.verifx.lemma.CvRDTProof
import probfilter.verifx.prover.Prover


class GCuckooFilterProof extends CvRDTProof {
  override protected val name: String = "GCuckooFilter"
  override protected val prover: Prover = new Prover()
  override protected val updateOps: Seq[String] = Seq.apply("add")
}

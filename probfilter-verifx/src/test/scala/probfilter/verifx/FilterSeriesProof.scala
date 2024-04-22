package probfilter.verifx

import probfilter.verifx.lemma.CvRDTProof
import probfilter.verifx.prover.Prover


class FilterSeriesProof extends CvRDTProof {
  override protected val name: String = "FilterSeriesProof"
  override protected val prover: Prover = new Prover()
  override protected val updateOps: Seq[String] = Seq.empty
}

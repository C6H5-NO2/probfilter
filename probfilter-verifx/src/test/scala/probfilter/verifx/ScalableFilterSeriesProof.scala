package probfilter.verifx

import probfilter.verifx.lemma.CvRDTProof
import probfilter.verifx.prover.Prover


class ScalableFilterSeriesProof extends CvRDTProof {
  override protected val name: String = "ScalableFilterSeries"
  override protected val prover: Prover = new Prover()
  override protected val updateOps: Seq[String] = Seq.empty
}

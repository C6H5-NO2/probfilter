package probfilter.verifx

import probfilter.verifx.lemma.CvRDTProof
import probfilter.verifx.prover.Prover


class GBloomFilterProof extends CvRDTProof {
  override protected val name: String = "GBloomFilter"
  override protected val prover: Prover = new Prover()
  override protected val updateOps: Seq[String] = Seq.apply("add")
}

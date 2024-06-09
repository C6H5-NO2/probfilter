package com.c6h5no2.probfilter.verifx

import com.c6h5no2.probfilter.verifx.lemma.CvRDTProof
import com.c6h5no2.probfilter.verifx.prover.Prover


class GBloomFilterProof extends CvRDTProof {
  override protected val name: String = "GBloomFilter"
  override protected val prover: Prover = new Prover()
  override protected val updateOps: Seq[String] = Seq.apply("add")
}

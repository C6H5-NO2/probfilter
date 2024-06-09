package com.c6h5no2.probfilter.verifx.lemma


trait InflationProof extends JoinSemilatticeProof {
  protected val updateOps: Seq[String]

  "update ops" should "be inflationary" in {
    updateOps.foreach { updateOp =>
      val res = prover.prove((name, s"${updateOp}IsInflationary"))
      assert(res.result == "Proved", s"when $updateOp resulting $res")
    }
  }
}

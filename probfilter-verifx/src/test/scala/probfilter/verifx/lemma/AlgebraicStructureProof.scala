package probfilter.verifx.lemma


trait AlgebraicStructureProof extends JoinSemilatticeProof {
  behavior of "merge"

  it should "be idempotent" in {
    val res = prover.prove((name, "mergeIsIdempotent"))
    assert(res.result == "Proved", res)
  }

  it should "be commutative" in {
    val res = prover.prove((name, "mergeIsCommutative"))
    assert(res.result == "Proved", res)
  }

  it should "be associative" in {
    val res = prover.prove((name, "mergeIsAssociative"))
    assert(res.result == "Proved", res)
  }

  it should "agree with compare" in {
    val res = prover.prove((name, "mergeMatchesCompare"))
    assert(res.result == "Proved", res)
  }
}

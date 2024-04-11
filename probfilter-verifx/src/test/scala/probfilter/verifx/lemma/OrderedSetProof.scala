package probfilter.verifx.lemma


trait OrderedSetProof extends JoinSemilatticeProof {
  behavior of "compare"

  it should "be reflexive" in {
    val res = prover.prove((name, "compareIsReflexive"))
    assert(res.result == "Proved", res)
  }

  it should "be antisymmetric" in {
    val res = prover.prove((name, "compareIsAntisymmetric"))
    assert(res.result == "Proved", res)
  }

  it should "be transitive" in {
    val res = prover.prove((name, "compareIsTransitive"))
    assert(res.result == "Proved", res)
  }

  it should "agree with merge" in {
    val res = prover.prove((name, "compareMatchesMerge"))
    assert(res.result == "Proved", res)
  }
}

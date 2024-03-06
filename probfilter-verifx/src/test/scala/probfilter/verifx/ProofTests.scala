package probfilter.verifx

import org.scalatest.flatspec.AnyFlatSpec


class ProofTests extends AnyFlatSpec {
  private val prover = new Prover()

  "GBloomFilter" should "be a CvRDT" in {
    val res = prover.prove(("GBloomFilter", "is_a_CvRDT"))
    assert(res.result == "Proved", res)
  }

  "GCuckooFilter" should "be a CvRDT" in {
    // todo
  }

  "ORCuckooFilter" should "be a CvRDT" in {
    // todo
  }
}

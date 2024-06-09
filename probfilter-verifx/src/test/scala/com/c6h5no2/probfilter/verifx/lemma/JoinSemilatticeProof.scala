package com.c6h5no2.probfilter.verifx.lemma

import com.c6h5no2.probfilter.verifx.prover.Prover
import org.scalatest.flatspec.AnyFlatSpecLike


trait JoinSemilatticeProof extends AnyFlatSpecLike {
  protected val name: String
  protected val prover: Prover
}

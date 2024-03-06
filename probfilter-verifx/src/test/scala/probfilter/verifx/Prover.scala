package probfilter.verifx

import org.verifx.Analysis.Proofs.{Aborted, ProofName, Proved, Rejected}
import org.verifx.Compiler.ProjectCompiler
import org.verifx.Utilities.Scanner

import java.nio.file.Path


final class Prover {
  private val files: Set[Path] = Scanner.scan().toSet
  private val pc: ProjectCompiler = ProjectCompiler.apply(files)

  def prove(proof: ProofName, maxTries: Int = 5, timeoutInSeconds: Int = 10): Prover.Result = {
    val proofRes = pc.checkProofForModel(proof, maxTries, timeoutInSeconds * 1000)
    try {
      proofRes match {
        case p: Proved => new Prover.Result("Proved", p.z3Program, "")
        case r: Rejected => new Prover.Result("Rejected", r.z3Program, r.toString())
        case a: Aborted => new Prover.Result("Aborted", a.z3Program, "")
      }
    } finally {
      proofRes.dispose()
    }
  }
}


object Prover {
  final class Result(val result: String, val z3Program: String, val counterExample: String)
}

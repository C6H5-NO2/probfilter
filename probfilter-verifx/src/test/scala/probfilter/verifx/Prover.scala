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
        case p: Proved => Prover.Result("Proved", "", p.z3Program)
        case r: Rejected => Prover.Result("Rejected", r.toString(), r.z3Program)
        case a: Aborted => Prover.Result("Aborted", "", a.z3Program)
      }
    } finally {
      proofRes.dispose()
    }
  }
}


object Prover {
  final case class Result(result: String, counterExample: String, z3Program: String)
}

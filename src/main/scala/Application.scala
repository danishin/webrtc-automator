import java.io.File

import build.Build
import fetch.Fetch
import update.Update
import util.Program.{Env, AppError}
import util.{Program, Helper, Platform}

import scalaz.{-\/, \/-}

object Application {
  import util.ProgramOps._
  def main(args: Array[String]) {
    val program: Program[Unit] = args match {
      case Array("fetch", platformStr) =>
        Platform.from(platformStr)
          .toProgram(AppError(""))
          .flatMap(Fetch.run)

      case Array("update", platformStr) =>
        Platform.from(platformStr)
          .toProgram(AppError(""))
          .flatMap(Update.run)

      case Array("build", platformStr, archStr) =>
        Platform.from(platformStr)
          .flatMap(p => p.getArch(archStr).map(a => (p, a)))
//          .toProgram(AppError(""))
          .flatMap { case (p, a) => Build.run(p: Platform)(p.getArch(archStr).get: p.Arch); ??? }
        // TODO: START FROM HERE!!!!!!! figure out why the above type inference doesn't work

        ???

      case _ => Program.error(AppError(""))
    }

    program.eval(Env(".", List())) match {
      case \/-(_) =>
      case -\/(e) => Helper.exitError(e.message)
    }
  }
}

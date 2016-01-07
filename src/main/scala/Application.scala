import java.io.File

import build.Build
import fetch.Fetch
import update.Update
import util.Program.{Env, AppError}
import util.{Program, Helper, Platform}

import scalaz.{-\/, \/-}

// REFER TO http://stackoverflow.com/questions/32763822/use-of-path-dependent-type-as-a-class-parameter
//private case class Box(p: Platform)(val a: p.Architecture)

object Application {
  import util.ProgramOps._
  def main(args: Array[String]) {
    val program: Program[Unit] = args match {
      case Array("fetch", platformStr) =>
        Platform.parse(platformStr)
          .toProgram(AppError(""))
          .flatMap(Fetch.run)

      case Array("update", platformStr) =>
        Platform.parse(platformStr)
          .toProgram(AppError(""))
          .flatMap(Update.run)

      case Array("build", platformStr, archStr) =>
//        Platform.parse(platformStr)
//          .flatMap(p => p.parseArch(archStr).map(a => (p, a)))
//          .toProgram(AppError(""))
//          // FIXME: This fails to compile because scala compiler conveniently forgets that `p` is `p.type` and simply opts to `Platform`. Not sure why this happens exactly but this seems to be the problem.
//          // FIXME: related to singleton type and path-dependent type
//          .flatMap { case (p, a) => Build.run(p)(a) }

//        Platform.parse(platformStr)
//          .flatMap(p => p.parseArch(archStr).map(a => Box(p)(a)))
//          .toProgram(AppError(""))
//          .flatMap(box => Build.run(box.p)(box.a))

        Platform.parse(platformStr)
          .flatMap(p => p.parseArch(archStr).map(a => (p, a)))
          .toProgram(AppError(""))
          // FIXME: workaround for now
          .flatMap { case (p, a) => Build.run(p)(a.asInstanceOf[p.Architecture]) }

      case _ => Program.error(AppError(""))
    }

    program.eval(Env(".", List())) match {
      case \/-(_) =>
      case -\/(e) => Helper.exitError(e.message)
    }
  }
}

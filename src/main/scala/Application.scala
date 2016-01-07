import build.Build
import fetch.Fetch
import update.Update
import util.Program.{Env, AppError}
import util.{Program, Platform}

import scalaz.{-\/, \/-}

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
        // TODO: we do lipo here
        Platform.parse(platformStr)
          .flatMap(p => p.parseArch(archStr))
          .toProgram(AppError(""))
          .flatMap(Build.run)

      case _ => Program.error(AppError(""))
    }

    program.eval(Env(".", List())) match {
      case \/-(_) =>
        println(s"${Console.BLUE}Success${Console.RESET}")
        sys.exit()
      case -\/(e) =>
        println(s"${Console.RED}Error: ${e.message}${Console.RESET}")
        sys.exit(1)
    }
  }
}

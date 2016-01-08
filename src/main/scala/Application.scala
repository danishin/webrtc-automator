import action.{Update, Fetch, Build}
import util.Program.{Env, AppError}
import util.{Program, Platform}

import scalaz.{-\/, \/-}

object Application {
  import util.ProgramOps._

  def main(args: Array[String]) {
    val program: Program[Unit] = args match {
      case Array("fetch", platformStr) =>
        Platform.parse(platformStr)
          .toProgram(AppError.just("Invalid Fetch Parameter"))
          .flatMap(Fetch.run)

      case Array("update", platformStr) =>
        Platform.parse(platformStr)
          .toProgram(AppError.just("Invalid Update Parameter"))
          .flatMap(Update.run)

      case Array("build", platformStr, archStr) =>
        // TODO: we do lipo here
        Platform.parse(platformStr)
          .flatMap(p => p.Architecture.parse(archStr))
          .toProgram(AppError.just("Invalid Build Parameter"))
          .flatMap(Build.run)

      case _ => Program.error(AppError.just("Invalid Parameter"))
    }

    program.eval(Env(".", List())) match {
      case \/-(_) =>
        println(s"${Console.BLUE}Success${Console.RESET}")
        sys.exit()
      case -\/(e) =>
        println(s"${Console.RED}Error: ${e.error}${Console.RESET}")
        sys.exit(1)
    }
  }
}

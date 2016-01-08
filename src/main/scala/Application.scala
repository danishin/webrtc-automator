import action.{Update, Fetch, Build}
import util.Program.{Env, AppError}
import util.{Helper, Program, Platform}

import scalaz.{-\/, \/-}

object Application extends Helper {
  import util.ProgramOps._

  def main(args: Array[String]) {
    val argsString = s"[${args.mkString(", ")}]"

    def echoInput(message: String) =
      echo(
        s"""
          |------------------------------------------------
          |$message
          |------------------------------------------------
        """.stripMargin)

    val program: Program[Unit] = args match {
      case Array("fetch", platformStr) => for {
        p <- Platform.parse(platformStr).toProgram(AppError.just(s"Invalid argument for 'fetch': $argsString"))
        _ <- echoInput(s"Fetch Entire WebRTC library for $p")
        _ <- Fetch.run(p)
      } yield ()

      case Array("update", platformStr) => for {
        p <- Platform.parse(platformStr).toProgram(AppError.just(s"Invalid argument for 'update': $argsString"))
        _ <- echoInput(s"Update WebRTC library for $p")
        _ <- Update.run(p)
      } yield ()

      // TODO: we do lipo here
      case Array("build", platformStr, archStr) => for {
        a <- Platform.parse(platformStr).flatMap(_.Architecture.parse(archStr)).toProgram(AppError.just(s"Invalid argument for 'build': $argsString"))
        _ <- echoInput(s"Build WebRTC archive file for $a")
        _ <- Build.run(a)
      } yield ()

      case _ => Program.error(AppError.just(s"Invalid argument: $argsString"))
    }

    program.eval(Env(".", Map())) match {
      case \/-(_) =>
        println(s"${Console.BLUE}Success!${Console.RESET}")
        sys.exit()
      case -\/(e) =>
        // Switch between stacktraces
//        println(s"${Console.RED}[Error] ${e.error}${Console.RESET}")
        println(s"${Console.RED}[Error] ${e.error.getMessage}${Console.RESET}")
        sys.exit(1)
    }
  }
}

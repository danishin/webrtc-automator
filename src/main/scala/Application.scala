import action.turn.{Bootstrap, EC2Info}
import action.webrtc.{Platform, Update, Fetch, Build}
import util.Program.{Env, AppError}
import util.{Helper, Program}

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

    val program: Program[Unit] = args.toList match {
      case "webrtc" :: xs => xs match {
        case "fetch" :: platformStr :: Nil => for {
          p <- Platform.parse(platformStr).toProgram(AppError.just(s"Invalid argument for 'fetch': $argsString"))
          _ <- echoInput(s"Fetch Entire WebRTC library for $p")
          _ <- Fetch.run(p)
        } yield ()

        case "update" :: platformStr :: Nil => for {
          p <- Platform.parse(platformStr).toProgram(AppError.just(s"Invalid argument for 'update': $argsString"))
          _ <- echoInput(s"Update WebRTC library for $p")
          _ <- Update.run(p)
        } yield ()

        case "build" :: platformStr :: archsStr :: Nil => for {
          as <- Platform.parse(platformStr).flatMap(_.Architecture.parse(archsStr)).toProgram(AppError.just(s"Invalid argument for 'build': $argsString"))
          _ <- echoInput(s"Will build WebRTC archive file for ${as.mkString(", ")}")
          _ <- Build.run(as)
        } yield ()

        case _ => Program.error(AppError.just(s"Invalid argument for 'webrtc': $argsString"))
      }

      case "turn" :: xs => xs match {
        case "bootstrap" :: Nil => for {
          json         <- parseConfigJson
          ec2Info <- (json \ "turn").validate[EC2Info].toProgram
          _ <- echoInput(
            s"""
              |Configuration
              |--------------
              |aws_access_key: ${ec2Info.aws_access_key}
              |aws_secret_key: ${ec2Info.aws_secret_key}
              |region: ${ec2Info.region}
              |ec2 instance type: ${ec2Info.instance_type}
              |ec2 keypair name: ${ec2Info.key_pair_name}
              |ec2 keypair private key location: ${ec2Info.key_pair_private_key_location}
            """.stripMargin)
          _            <- Bootstrap.run(ec2Info)
        } yield ()

        case _ => Program.error(AppError.just(s"Invalid argument for 'turn': $argsString"))
      }

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

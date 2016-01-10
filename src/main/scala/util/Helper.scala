package util

import java.io.File

import util.Program.{AppError, Env}

import scalaz._

trait Helper extends ProgramFunctions {
  import sys.process.Process
  import java.nio.charset.StandardCharsets
  import java.nio.file.{Paths, Files}

  abstract class PathRep(val path: String) {
    def apply(p: String): String = s"$path/$p"
  }

  private object PathRep {
    import scala.language.implicitConversions
    implicit def pathRep2String(pathRep: PathRep): String = pathRep.path
  }

  // Type-safe tree traversal
  object root extends PathRep(sys.env("PWD")) {
    object lib extends PathRep(root("lib")) {
      object depot_tools extends PathRep(lib("depot_tools"))
      object src extends PathRep(lib("src"))
    }

    object output extends PathRep(root("output")) {
      object `WebRTCiOS.framework` extends PathRep(output("WebRTCiOS.framework")) {
        object Versions extends PathRep(`WebRTCiOS.framework`("Versions")) {
          object A extends PathRep(Versions("A")) {
            object Headers extends PathRep(A("Headers"))
          }
        }
      }
    }

    object resources extends PathRep(root("resources")) {
      object `RTCTypes.h` extends PathRep(resources("RTCTypes.h"))
    }

    object tmp extends PathRep(root("tmp"))
  }

  object Color extends Enumeration {
    val Cyan = Value(Console.CYAN)
    val Blue = Value(Console.BLUE)
  }
  private def log(message: String, color: Color.Value): Unit = println(s"${color.toString}$message${Console.RESET}")

  def echo(m: String): Program[Unit] = Program(log(m, Color.Blue))

  def shell(commandArgs: String*): Program[Unit] = {
    // NB: `/bin/sh -c` is needed to support shell features like globbing. And it accepts one string so we `mkString`. And we wrap every argument with quote to avoid parameter expansion wrt whitespace.
    val command = Seq("/bin/sh", "-c") :+ commandArgs/*.map(a => s"'$a'")*/.mkString(" ")

    for {
      _ <- Program((env: Env) => log(s"""
          |Executing...
          |-------------------------
          |- Command: ${command.mkString(" ")}
          |- CWD: ${env.cwd}
          |- Env Vars: ${env.envVars.mkString("\n", "\n", "")}
          |-------------------------
        """.stripMargin, Color.Cyan))

      _ <- Program((env: Env) =>
        Process(command, new File(env.cwd), env.envVars.toList: _*).! match {
          case 0 => \/-(())
          case exit_code => throw AppError.just(s"Command '${command.mkString(" ")}' exited with exit code $exit_code")
        }
      )
    } yield ()
  }

  // TODO: Replace all these with just shell command?
  def copy(from: String, to: String): Program[Unit] = Program(Files.copy(Paths.get(from), Paths.get(to))).map(_ => ())

  def write(path: String, content: String): Program[Unit] = Program(Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8))).map(_ => ())

  def emptyDir(path: String): Program[Unit] = Program(new File(path).listFiles.foreach(_.delete()))
}

object Helper extends Helper
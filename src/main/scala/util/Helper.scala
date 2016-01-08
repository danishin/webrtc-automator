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
    object webrtc extends PathRep(root("webrtc")) {
      object depot_tools extends PathRep(webrtc("depot_tools"))
      object src extends PathRep(webrtc("src"))
    }

    object output extends PathRep(root("output")) {
      object headers extends PathRep(output("headers")) {
        object ios extends PathRep(headers("ios"))
      }
    }

    object tmp extends PathRep(root("tmp"))
  }

  def resource_path(name: String): String = getClass.getResource(name).getPath

  def echo(f: Env => String): Program[Unit] = Program(env => println(s"${Console.CYAN}${f(env)}${Console.RESET}"))

  def echo(m: String): Program[Unit] = Program(println(s"${Console.CYAN}$m${Console.RESET}"))

  def appendToEnv(key: String, value: String) = sys.env.get(key) match {
    case Some(v) if v.nonEmpty =>
      val separator = System.getProperty("path.separator")
      s"$v$separator$value"
    case None => value
  }

  def shell(commands: String*): Program[Unit] = for {
    _ <- echo(env => s"""
                        |Executing...
                        |${commands.mkString(" ")}
                        |cwd: ${env.cwd}
                        |envs: ${env.envVars}
        """.stripMargin)

    _ <- Program.wrap { env =>
      Process(commands, new File(env.cwd), env.envVars: _*).! match {
        case 0 => \/-(())
        case exit_code => -\/(AppError.just(s"Command '${commands.mkString(" ")}' exited with exit code $exit_code"))
      }
    }
  } yield ()

  def copy(from: String, to: String): Program[Unit] = Program(Files.copy(Paths.get(from), Paths.get(to))).map(_ => ())

  def write(path: String, content: String): Program[Unit] = Program(Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8))).map(_ => ())

  def emptyDir(path: String): Program[Unit] = Program(new File(path).listFiles.foreach(_.delete()))
}

object Helper extends Helper
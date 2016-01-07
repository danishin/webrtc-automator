package util

import java.io.File

import util.Program.{AppError, Env}

import scalaz._

trait Helper extends ProgramFunctions {
  import sys.process.Process
  import java.nio.charset.StandardCharsets
  import java.nio.file.{Paths, Files}

  val PWD = sys.env("PWD")

  def echo(f: Env => String): Program[Unit] = Program.just { env =>
    println(s"${Console.CYAN}${f(env)}${Console.RESET}")
  }

  def echo(m: String): Program[Unit] = Program.only(println(s"${Console.CYAN}$m${Console.RESET}"))

  def appendToEnv(key: String, value: String) = sys.env.get(key) match {
    case Some(v) if v.nonEmpty =>
      val separator = System.getProperty("path.separator")
      s"$v$separator$value"
    case None => value
  }

  // FIXME: Accept variadic arguments and construct using array only to avoid weird behaviors with whitespaces
  def shell(command: String): Program[Unit] = for {
    _ <- echo(env => s"""
                        |Executing...
                        |$command
                        |cwd: ${env.cwd}
                        |envs: ${env.envVars}
        """.stripMargin)

    _ <- Program { env =>
      Process(command, new File(env.cwd), env.envVars: _*).! match {
        case 0 => \/-(())
        case exit_code => -\/(AppError(s"Command '$command' exited with exit code $exit_code"))
      }
    }
  } yield ()

  def copy(from: String, to: String): Program[Unit] = Program.only(Files.copy(Paths.get(from), Paths.get(to)))
  def write(path: String, content: String): Program[Unit] = Program.only(Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8)))
  def emptyDir(path: String): Program[Unit] = Program.only(new File(path).listFiles.foreach(_.delete()))
}

object Helper extends Helper
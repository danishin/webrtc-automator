package util

import java.io.File

import scalaz._

case class Env(cwd: String, envVars: List[(String, String)])
case class Error(message: String) extends AnyVal

trait Helper {
  val PWD = sys.env("PWD")

  private def echo(m: String) = println(s"${Console.CYAN}$m${Console.RESET}")

  def exitError(m: String) = {
    println(s"${Console.RED}Error: $m${Console.RESET}")
    sys.exit(1)
  }

  def appendToEnv(key: String, value: String) = sys.env.get(key) match {
    case Some(v) if v.nonEmpty =>
      val separator = System.getProperty("path.separator")
      s"$v$separator$value"
    case None => value
  }

  implicit class StringExt(string: String) {
    import sys.process.Process

    def !(logMessage: String): State[Env, Unit] = State { env =>
      echo(logMessage)

      echo(s"""
           |Executing...
           |$string
           |cwd: ${env.cwd}
           |envs: ${env.envVars}
        """.stripMargin)

      type A = StateT[({ type l[X] = \/[Error, X] })#l, Env, Unit]

      val a: A = ???
      a.flatMap(_ => StateT { (env: Env) =>
        val a: \/[Error, Env] = -\/(Error(""))
        a
      })

      Process(string, new File(env.cwd), env.envVars: _*).! match {
        case 0 => (env, None)
        case exit_code => (env, Some(Error(s"Command '$string' exited with exit code $exit_code")))
      }
    }
  }

//  def exec(cwd: String, envs: Envs = List())(f: File => Envs => Unit) = f(new File(cwd))(envs)

  import java.nio.charset.StandardCharsets
  import java.nio.file.{Paths, Files}

  def copy(from: String, to: String): Unit = Files.copy(Paths.get(from), Paths.get(to))
  def write(path: String, content: String): Unit = Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8))
  def emptyDir(path: String): Unit = new File(path).listFiles.foreach(_.delete())
}

object Helper extends Helper
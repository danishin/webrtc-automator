package util

import java.io.File

trait Helper {
  type Envs = List[(String, String)]

  val PWD = sys.env("PWD")

  def echo(m: String) = {
    println(s"${Console.CYAN}$m${Console.RESET}")
  }

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

    def !(implicit cwd: File, envs: Envs): Unit = {
      echo(
        s"""
           |Executing...
           |$string
           |cwd: $cwd
           |envs: $envs
        """.stripMargin)
      val exitCode = Process(string, cwd, envs: _*).!
      if(exitCode != 0) exitError(s"Command '$string' exited with exit code $exitCode")
    }
  }

  def exec(cwd: String, envs: Envs = List())(f: File => Envs => Unit) = f(new File(cwd))(envs)

  import java.nio.charset.StandardCharsets
  import java.nio.file.{Paths, Files}

  def copy(from: String, to: String): Unit = Files.copy(Paths.get(from), Paths.get(to))
  def write(path: String, content: String): Unit = Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8))
  def emptyDir(path: String): Unit = new File(path).listFiles.foreach(_.delete())
}

object Helper extends Helper
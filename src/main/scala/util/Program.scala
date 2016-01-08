package util

import scalaz._

class Program[A] private[util] (val boxState: Program.BoxState[A]) {
  import Program._

  def map[B](f: A => B): Program[B] = monad.map(this)(f)

  def flatMap[B](f: A => Program[B]): Program[B] = monad.bind(this)(f)

  def eval(initial: Env): Box[A] = boxState.eval(initial)

  def exec(initial: Env): Box[Env] = boxState.exec(initial)

  def run(initial: Env): Box[(Env, A)] = boxState.run(initial)
}

object Program {
  case class Env(cwd: String, envVars: List[(String, String)])
  case class AppError(error: Throwable) extends Throwable
  object AppError {
    def just(message: String): AppError = AppError(new Throwable(message))
  }

  type E = AppError
  private[util] type Box[S] = E \/ S
  private[util] type BoxState[A] = StateT[Box, Env, A]

  def wrap[A](f: Env => Box[A]): Program[A] = new Program(StateT[Box, Env, A](env => f(env).map((env, _))))

  def just[A](a: A): Program[A] = wrap(_ => \/-(a))

  def error[A](e: E): Program[A] = wrap(_ => -\/(e))

  def apply[A](a: => A): Program[A] = apply(_ => a)

  def apply[A](f: Env => A): Program[A] = wrap(env => TryRun(f(env), AppError.apply))

  /**
    * Utility function to execute a block and wrap it in `Box[A]`
    */
  private def TryRun[A](a: => A, f: (Throwable => E)): Box[A] =
    try \/-(a)
    catch {
      case e: E => -\/(e)
      case scala.util.control.NonFatal(error) => -\/(f(error)) // NOTE: Must not catch fatal errors.
    }

  /* Instances */
  implicit val monad = new Monad[Program] {
    override def point[A](a: => A): Program[A] = just(a)
    override def bind[A, B](fa: Program[A])(f: (A) => Program[B]): Program[B] =
      new Program(fa.boxState.flatMap(a => f(a).boxState))
  }
}

trait ProgramFunctions {
  import Program._

  def putEnv(e: Env): Program[Unit] = modifyEnv(_ => e)
  def modifyEnv(f: Env => Env): Program[Unit] = new Program(StateT[Box, Env, Unit](env => \/-(f(env), ())))
}

trait ProgramOps {
  import Program._

  implicit class OptionExt[A](option: Option[A]) {
    def toProgram(e: E): Program[A] = option.fold[Program[A]](Program.error(e))(Program.just)
  }
}

object ProgramOps extends ProgramOps
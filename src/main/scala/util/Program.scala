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
  case class AppError(message: String)

  private[util] type Box[S] = AppError \/ S
  private[util] type BoxState[A] = StateT[Box, Env, A]

  def only[A](a: => A): Program[A] = just(_ => a)

  def just[A](f: Env => A): Program[A] = new Program(StateT[Box, Env, A](env => \/-(env, f(env))))

  def error[A](e: AppError): Program[A] = new Program(StateT[Box, Env, A](env => -\/(e)))

  def apply[A](f: Env => Box[A]): Program[A] = new Program(StateT[Box, Env, A](env => f(env).map(a => (env, a))))

  /* Instances */
  implicit val monad = new Monad[Program] {
    override def point[A](a: => A): Program[A] = just(_ => a)
    override def bind[A, B](fa: Program[A])(f: (A) => Program[B]): Program[B] =
      new Program(fa.boxState.flatMap(a => f(a).boxState))
  }
}

object ProgramFunctions {
  import Program._

  def modifyEnv(f: Env => Env): Program[Unit] = new Program(StateT[Box, Env, Unit](env => \/-(f(env), ())))
}

trait ProgramOps {
  import Program._

  implicit class OptionExt[A](option: Option[A]) {
    def toProgram(e: AppError): Program[A] = option.fold[Program[A]]( Program.error(e) )( Program.only )
  }
}

object ProgramOps extends ProgramOps
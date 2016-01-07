package build

import util.{Program, Platform}

import scalaz.{State, Reader, ReaderT, Kleisli}
import scalaz.effect.{MonadIO, IO}


object Build {
  def run(platform: Platform)(arch: platform.Arch): Program[Unit] = platform match {
    case Platform.IOS =>
      ???

    case Platform.Android =>
      ???
  }
}

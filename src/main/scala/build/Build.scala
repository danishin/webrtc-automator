package build

import util.Platform

import scalaz.{State, Reader, ReaderT, Kleisli}
import scalaz.effect.{MonadIO, IO}


object Build {
  def run(platform: Platform)(arch: platform.Arch) = platform match {
    case Platform.IOS =>

    case Platform.Android =>
  }
}

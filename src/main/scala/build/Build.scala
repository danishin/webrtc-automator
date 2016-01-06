package build

import util.Platform

import scalaz.{Kleisli, Reader}
import scalaz.effect.{MonadIO, IO}

object Build {
  def run(platform: Platform)(arch: platform.Arch) = platform match {
    case Platform.IOS =>
      // TODO: START FROM HERE!!!!!!!!!!!!!! use reader monad to pass around env
      // TODO: START FROM HERE!!!!!!!!!!! USE IO MONAD
      Reader


    case Platform.Android =>
  }
}

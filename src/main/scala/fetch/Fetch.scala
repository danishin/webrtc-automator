package fetch

import util.{Env, Helper, Platform}

import scalaz.State

object Fetch extends Helper {
  def modifyEnv(env: Env => Env): State[Env, Unit] = ???

  def run(platform: Platform) = platform match {
    case Platform.IOS =>
      val a = for {
        _ <- modifyEnv(_.copy(cwd = "./webrtc"))
        _ <- "git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git webrtc" ! "Clone depot_tools"

        _ <- modifyEnv(_.copy(envVars = List("PATH" -> appendToEnv("PATH", s"$PWD/webrtc/depot_tools"), "GYP_DEFINES" -> "OS=ios")))
        aa <- "fetch --nohooks webrtc_ios" ! "Start Fetching WebRTC source"
        _ <- "gclient sync" ! "Update Dependencies"
      } yield ()

    case Platform.Android =>
  }
}

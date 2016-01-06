package fetch

import util.{Helper, Platform}

import scalaz.effect.IO

object Fetch extends Helper {
  def run(platform: Platform) = platform match {
    case Platform.IOS =>
      for {
        _ <- echo("")
        _ <- IO("git clone".!)

      } yield ()

      exec("./webrtc") { implicit c => implicit e =>
        echo("Clone depot_tools")
        "git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git webrtc".!
      }

      val envs = List(
        "PATH" -> appendToEnv("PATH", s"$PWD/webrtc/depot_tools"),
        "GYP_DEFINES" -> "OS=ios"
      )

      exec("./webrtc", envs) { implicit c => implicit e =>
        echo("Start Fetching WebRTC source")
        "fetch --nohooks webrtc_ios".!

        echo("Update Dependencies")
        "gclient sync".!
      }

    case Platform.Android =>
  }
}

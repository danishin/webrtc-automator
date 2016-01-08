package action

import util.{Helper, Platform, Program}

object Fetch extends Helper {
  def run(platform: Platform): Program[Unit] = platform match {
    case Platform.IOS =>
      for {
        _ <- modifyEnv(_.copy(cwd = root.lib))
        _ <- echo("Clone depot_tools")
        _ <- shell("git", "clone", "https://chromium.googlesource.com/chromium/tools/depot_tools.git")

        _ <- modifyEnv(_.copy(envVars = Map("GYP_DEFINES" -> "OS=ios")))
        _ <- echo("Start Fetching WebRTC source")
        _ <- shell("fetch", "--nohooks", "webrtc_ios")

        _ <- echo("Update Dependencies")
        _ <- shell("gclient", "sync")
      } yield ()

    case Platform.Android => ???
  }
}

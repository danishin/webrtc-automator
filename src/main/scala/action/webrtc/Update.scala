package action.webrtc

import util.Program.Env
import util.{Helper, Program}

object Update extends Helper {

  def run(platform: Platform): Program[Unit] = platform match {
    case Platform.IOS =>
      for {
        _ <- putEnv(Env(root.lib.src, Map("GYP_DEFINES" -> "OS=ios")))
        _ <- echo("Pull the latest WebRTC source")
        _ <- shell("git", "pull")

        _ <- modifyEnv(_.copy(cwd = root.lib))
        _ <- echo("Update the build toolchain and all dependencies")
        _ <- shell("gclient", "sync")
      } yield ()

    case Platform.Android => ???
  }
}

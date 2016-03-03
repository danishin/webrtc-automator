package action.webrtc

import util.Program.Env
import util.{Helper, Program}

object Update extends Helper {

  def run(platform: Platform): Program[Unit] = platform match {
    case Platform.IOS =>
      for {
        _ <- echo("Pull the latest depot_tools")
        _ <- putEnv(Env(root.lib.depot_tools, Map()))
        _ <- shell("git", "pull")

        _ <- putEnv(Env(root.lib, Map("GYP_DEFINES" -> "OS=ios")))
        _ <- echo("Configure gclient for iOS build")
        _ <- shell("gclient", "config", "--unmanaged", "--name=src", "https://chromium.googlesource.com/external/webrtc")

        _ <- echo("Write target_os in .gclient generated by gclient config")
        _ <- append(root.lib.`.gclient`, "target_os = ['ios', 'mac']")

        _ <- echo("Sync WebRTC to latest release branch")
        _ <- shell("gclient", "sync", "--with_branch_heads")
      } yield ()

    case Platform.Android => ???
  }
}

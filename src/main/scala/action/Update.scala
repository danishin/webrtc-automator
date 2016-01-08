package action

import java.io.File

import util.Program.Env
import util.{Helper, Platform, Program}

object Update extends Helper {
  import scalaz.Scalaz._

  def run(platform: Platform): Program[Unit] = platform match {
    case Platform.IOS =>
      for {
        _ <- putEnv(Env(root.webrtc.src, List("PATH" -> appendToEnv("PATH", root.webrtc.depot_tools), "GYP_DEFINES" -> "OS=ios")))
        _ <- echo("Pull the latest WebRTC source")
        _ <- shell("git", "pull")

        _ <- modifyEnv(_.copy(cwd = root.webrtc))
        _ <- echo("Update the build toolchain and all dependencies")
        _ <- shell("gclient", "sync")

        // NB: Exclude `RTCNS*.h` since these are OSX-specific
        webrtcHeaderFiles = new File(root.webrtc.src("talk/app/webrtc/objc/public")).listFiles.filter(_.getName.matches("""RTC(?!NS).*\.h""")).toList

        _ <- echo("Copy header files")
        _ <- emptyDir(root.output.headers.ios)
        _ <- webrtcHeaderFiles.traverse_[Program](f => copy(f.getAbsolutePath, root.output.headers.ios(f.getName)))

        _ <- echo("Create libjingle-umbrella.h")
        _ <- write(root.output.headers.ios("libjingle-umbrella.h"), {
          val basename = """(.+)\.h""".r
          List(
            "#import <UIKit/UIKit.h>",
            "",
            webrtcHeaderFiles.map(_.getName match { case basename(name) => s"""#import "$name"""" }),
            "",
            "FOUNDATION_EXPORT double libjingle_peerconnectionVersionNumber;",
            "FOUNDATION_EXPORT const unsigned char libjingle_peerconnectionVersionString[];"
          ).mkString("\n")
        })

        _ <- echo("Copy RTCTypes.h")
        _ <- copy(resource_path("RTCTypes.h"), root.output.headers.ios("RTCTypes.h"))

      } yield ()

    case Platform.Android => ???
  }
}

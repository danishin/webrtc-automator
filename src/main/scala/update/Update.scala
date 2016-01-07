package update

import java.io.File

import util.Program.Env
import util.{Program, Helper, Platform}

import scalaz.State

object Update extends Helper {
  import scalaz._
  import Scalaz._
  import util.ProgramFunctions._

  // FIXME:
  def targetHeaderFilePath(name: String) = s"$PWD/target/headers/ios/$name"

  def run(platform: Platform): Program[Unit] = platform match {
    case Platform.IOS =>
      for {
        _ <- modifyEnv(_ => Env("./webrtc/src", List("PATH" -> appendToEnv("PATH", s"$PWD/webrtc/depot_tools"), "GYP_DEFINES" -> "OS=ios")))
        _ <- echo("Pull the latest WebRTC source")
        _ <- shell("git pull")

        _ <- modifyEnv(_.copy(cwd = "./webrtc"))
        _ <- echo("Update the build toolchain and all dependencies")
        _ <- shell("gclient sync")

        // NB: Exclude `RTCNS*.h` since these are OSX-specific
        webrtcHeaderFiles = new File(s"$PWD/webrtc/src/talk/app/webrtc/objc/public").listFiles.filter(_.getName.matches("""RTC(?!NS).*\.h""")).toList

        _ <- echo("Empty target/headers")
        _ <- emptyDir(s"$PWD/target/headers")

        _ <- echo("Copy header files")
        _ <- webrtcHeaderFiles.traverse_[Program](f => copy(f.getAbsolutePath, targetHeaderFilePath(f.getName)))

        _ <- echo("Create libjingle-umbrella.h")
        _ <- write(targetHeaderFilePath("libjingle-umbrella.h"), {
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
        _ <- copy(getClass.getResource("RTCTypes.h").getPath, targetHeaderFilePath("RTCTypes.h"))

      } yield ()
  }

}

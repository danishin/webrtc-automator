package update

import java.io.File

import util.{Helper, Platform}

object Update extends Helper {
  def run(platform: Platform) = platform match {
    case Platform.IOS =>
      val envs = List(
        "PATH" -> appendToEnv("PATH", s"$PWD/webrtc/depot_tools"),
        "GYP_DEFINES" -> "OS=ios"
      )

      exec("./webrtc/src", envs) { implicit c => implicit e =>
        echo("Pull the latest WebRTC source")
        "git pull".!
      }

      exec("./webrtc", envs) { implicit c => implicit e =>
        // Paths
        val webrtcHeadersDir = s"$PWD/webrtc/src/talk/app/webrtc/objc/public"
        def targetHeaderFilePath(name: String) = s"$PWD/target/headers/ios/$name"

        // Start
        echo("Update the build toolchain and all dependencies")
        "gclient sync".!

        val webrtcHeaderFiles = new File(webrtcHeadersDir)
          .listFiles
          .filter(_.getName.matches("""RTC(?!NS).*\.h""")) // NB: Exclude `RTCNS*.h` since these are OSX-specific

        echo("Empty target/headers")
        emptyDir(s"$PWD/target/headers")

        echo("Copy header files")
        webrtcHeaderFiles.foreach(f => copy(f.getAbsolutePath, targetHeaderFilePath(f.getName)))

        echo("Create libjingle-umbrella.h")
        write(targetHeaderFilePath("libjingle-umbrella.h"), {
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

        echo("Copy RTCTypes.h")
        copy(getClass.getResource("RTCTypes.h").getPath, targetHeaderFilePath("RTCTypes.h"))
      }

    case Platform.Android =>
  }

}

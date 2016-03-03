package action.webrtc

import java.io.File
import java.nio.file.{Paths, Files}

import util.{Program, Helper}

object Assemble extends Helper {
  import scalaz.Scalaz._

  def run(archs: List[Platform#Architecture]): Program[Unit] = archs.head.platform match {
    case Platform.IOS => for {
      _ <- Program.guard(archs.forall(a => Files.exists(Paths.get(a.archive_file_path))))("Archive files don't exist. Please run build before running assemble")

      _ <- echo("Create 'WebRTCiOS.framework'")
      _ <- shell("rm", "-rf", root.output.`WebRTCiOS.framework`)
      _ <- shell("mkdir", "-p", root.output.`WebRTCiOS.framework`.Versions.A.Headers)

      _ <- shell("lipo", "-output", root.output.`WebRTCiOS.framework`.Versions.A("WebRTCiOS"), "-create", archs.map(_.archive_file_path).mkString(" "))

      // NB: Exclude `RTCNS*.h` since these are OSX-specific
      webrtcHeaderFiles = new File(root.lib.src("talk/app/webrtc/objc/public")).listFiles.filter(_.getName.matches("""RTC(?!NS).*\.h""")).toList

      _ <- webrtcHeaderFiles.traverse_[Program](f => shell("cp", f.getAbsolutePath, root.output.`WebRTCiOS.framework`.Versions.A.Headers(f.getName)))
      _ <- shell("cp", root.resources.`RTCTypes.h`, root.output.`WebRTCiOS.framework`.Versions.A.Headers("RTCTypes.h"))
      _ <- write(root.output.`WebRTCiOS.framework`.Versions.A.Headers("WebRTCiOS.h"), {
        List(
          "#import <UIKit/UIKit.h>",
          "",
          webrtcHeaderFiles.map(f => s"""#import "${f.getName}"""").mkString("\n"),
          "",
          "FOUNDATION_EXPORT double libjingle_peerconnectionVersionNumber;",
          "FOUNDATION_EXPORT const unsigned char libjingle_peerconnectionVersionString[];"
        ).mkString("\n")
      })

      _ <- shell("ln", "-sfh", root.output.`WebRTCiOS.framework`.Versions.A, root.output.`WebRTCiOS.framework`.Versions("Current"))
      _ <- shell("ln", "-sfh", root.output.`WebRTCiOS.framework`.Versions("Current/Headers"), root.output.`WebRTCiOS.framework`("Headers"))
      _ <- shell("ln", "-sfh", root.output.`WebRTCiOS.framework`.Versions("Current/WebRTCiOS"), root.output.`WebRTCiOS.framework`("WebRTCiOS"))
    } yield ()

    case Platform.Android =>
      ???
  }
}

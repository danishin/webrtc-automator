package action

import java.io.File

import action.Update._
import util.Program.Env
import util.{Helper, Platform, Program}

object Build extends Helper {
  import scalaz.Scalaz._

  def run(archs: List[Platform#Architecture]): Program[Unit] = archs.head.platform match {
    case Platform.IOS => for {
      archive_file_paths <- archs.traverse(build_ios)

      _ <- echo("Create 'WebRTCiOS.framework'")
      _ <- shell("rm", "-rf", root.output.`WebRTCiOS.framework`)
      _ <- shell("mkdir", "-p", root.output.`WebRTCiOS.framework`.Versions.A.Headers)

      _ <- shell("lipo", "-output", root.output.`WebRTCiOS.framework`.Versions.A("WebRTCiOS"), "-create", archive_file_paths.mkString(" "))

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

  private def build_ios(arch: Platform#Architecture): Program[String] = for {
    _ <- echo(s"Build archive file for $arch")

    _ <- putEnv(Env(
      cwd = root.lib.src,
      envVars = Map(
        "GYP_CROSSCOMPILE" -> "1",
        "GYP_GENERATORS" -> "ninja",
        "GYP_DEFINES" -> s"OS=ios target_arch=${arch.target_arch} build_with_libjingle=1 build_with_chromium=0",
        "GYP_GENERATOR_FLAGS" -> s"output_dir=${arch.output_dir_name}"
      )))

    output_dir = root.lib.src(arch.output_dir_name)
    _ <- shell("rm", "-rf", output_dir)

    _ <- echo("Generate New Build File")
    _ <- shell("python", "webrtc/build/gyp_webrtc")

    output_flavor_dir = s"$output_dir/${arch.flavor}"
    _ <- echo("Start Compiling")
    _ <- shell("ninja", "-C", output_flavor_dir, arch.extra_ninja_build_flag.getOrElse(""), "AppRTCDemo")

    output_flavor_archive_file = s"$output_flavor_dir/${arch.archive_file_name}"
    _ <- echo("Produce Statically Linked Archive File From the Input Files")
    _ <- shell("libtool", "-static", "-o", output_flavor_archive_file, s"$output_flavor_dir/*.a")

    _ <- modifyEnv(_.copy(cwd = "."))
    _ <- echo("Strip Archive File of Unnecessary Symbols and Write to Tmp Folder")
    _ <- shell("mkdir", "-p", root.tmp)
    // -S: Remove the debugging symbol table entries
    // -x: Remove all local symbols (saving only global symbols)
    archive_file_path = root.tmp(arch.archive_file_name)
    _ <- shell("rm", "-f", archive_file_path)
    _ <- shell("strip", "-S", "-x", "-o", archive_file_path, output_flavor_archive_file)
  } yield archive_file_path
}

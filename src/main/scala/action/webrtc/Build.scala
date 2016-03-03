package action.webrtc

import java.io.File

import util.Program.Env
import util.{Helper, Program}

object Build extends Helper {
  def run(arch: Platform#Architecture): Program[Unit] = arch.platform match {
    case Platform.IOS => for {
      _ <- echo(s"Build archive file for $arch")

      _ <- putEnv(Env(
        cwd = root.lib.src,
        envVars = Map(
          "GYP_CROSSCOMPILE" -> "1",
          "GYP_GENERATORS" -> "ninja",
          // `clang_xcode=1` is needed because chromium project is using old version of clang. Refer to https://bugs.chromium.org/p/webrtc/issues/detail?id=5182
          "GYP_DEFINES" -> s"OS=ios target_arch=${arch.target_arch} build_with_libjingle=1 build_with_chromium=0 clang_xcode=1",
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
      _ <- echo("Strip Archive File of Unnecessary Symbols and Write to output/archive/")
      _ <- shell("mkdir", "-p", root.output.archive)
      // -S: Remove the debugging symbol table entries
      // -x: Remove all local symbols (saving only global symbols)
      archive_file_path = root.output.archive(arch.archive_file_name)
      _ <- shell("rm", "-f", archive_file_path)
      _ <- shell("strip", "-S", "-x", "-o", archive_file_path, output_flavor_archive_file)
    } yield ()

    case Platform.Android =>
      ???
  }
}

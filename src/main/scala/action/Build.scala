package action

import util.Program.Env
import util.{Helper, Platform, Program}

object Build extends Helper {
  def run(arch: Platform#Architecture): Program[Unit] = arch.platform match {
    case Platform.IOS => for {
      _ <- putEnv(Env(
        cwd = root.lib.src,
        envVars = Map(
          "GYP_CROSSCOMPILE" -> "1",
          "GYP_GENERATORS" -> "ninja",
          "GYP_DEFINES" -> s"OS=ios target_arch=${arch.target_arch} build_with_libjingle=1 build_with_chromium=0",
          "GYP_GENERATOR_FLAGS" -> s"output_dir=${arch.output_dir_name}"
        )))

      _ <- echo("Generate New Build File")
      _ <- shell("python", "webrtc/build/gyp_webrtc")

      output_flavor_dir = root.lib.src(s"${arch.output_dir_name}/${arch.flavor}")
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
      _ <- shell("strip", "-S", "-x", "-o", root.tmp(arch.archive_file_name), output_flavor_archive_file)
    } yield ()

    case Platform.Android =>
      ???
  }
}

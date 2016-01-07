package build

import util.{Helper, Program, Platform}

object Build extends Helper {
  def run(arch: Platform#Architecture): Program[Unit] = arch.platform match {
    case Platform.IOS => for {
      _ <- modifyEnv(_.copy(
        cwd = "webrtc/src",
        envVars = List(
          "PATH" -> appendToEnv("PATH", s"$PWD/webrtc/depot_tools"),
          "GYP_CROSSCOMPILE" -> "1",
          "GYP_GENERATORS" -> "ninja",
          "GYP_DEFINES" -> s"OS=ios target_arch=${arch.target_arch} build_with_libjingle=1 build_with_chromium=0",
          "GYP_GENERATOR_FLAGS" -> s"output_dir=${arch.output_dir}"
        )))

      _ <- echo(s"Run GYP Generator Script")
      _ <- shell("webrtc/build/gyp_webrtc")

      // FIXME: Use absolute path always!!!!
      output_flavor_dir = s"${arch.output_dir}/${arch.flavor}"
      _ <- echo("Start Compiling")
      _ <- shell(s"ninja -C $output_flavor_dir ${arch.extra_ninja_build_flag.getOrElse("")} AppRTCDemo")

      // FIXME: Use absolute path always!!!!
      output_flavor_archive_file = s"$output_flavor_dir/${arch.archive_file_name}"
      _ <- echo("Produce static FAT archive file")
      _ <- shell(s"libtool -static -o $output_flavor_archive_file $output_flavor_dir/*.a")

      _ <- modifyEnv(_.copy(cwd = "."))
      _ <- echo("Strip FAT archive file and move to tmp folder")
    // FIXME: Use absolute path always!!!!
      _ <- shell(s"strip -S -x -o tmp/${arch.archive_file_name} webrtc/src/$output_flavor_archive_file")
    } yield ()

    case Platform.Android =>
      ???
  }
}

package action.webrtc

import util.Program.Env
import util.{Helper, Program}

object BuildType extends Enumeration {
  val Debug = Value("Debug")
  val Release = Value("Release")

  def from(str: String): Option[BuildType.Value] = str match {
    case "DEBUG" => Some(Debug)
    case "RELEASE" => Some(Release)
    case _ => None
  }
}

object Build extends Helper {
  private val IOS_DEPLOYMENT_TARGET = 8.0
  private val BuildTarget = "WebRTCiOS" // change this to `AppRTCDemo` for running example app.

  def run(arch: Platform#Architecture, buildType: BuildType.Value): Program[Unit] = arch.platform match {
    case Platform.IOS => for {
      _ <- echo(s"Build archive file for $arch with deploy target of $IOS_DEPLOYMENT_TARGET $buildType")

      /**
        * - Exclude unnecessary sources (the ones only needed for `AppRTCDemo` - check its target in `webrtc_examples.gyp` for an example)
        * // - Use new WebRTC api for Objective-C (these apis are technically still work-in-progress but I simply cannot deal with all these delegate any further - it is likely that this build file will only be updated after AppRTCDemo is completely rewritten to new api, which wont be any soon.)
        * // New API doesn't work for now. Use old API for now and track commit history of `webrtc/api/objc`
        *
        * Reference: https://github.com/pristineio/webrtc-build-scripts/blob/master/ios/insert_two_lines_after_text.py
        */
      _ <- echo("Inject `WebRTCiOS` target to `webrtc/webrtc_examples.gyp`")
      original_webrtc_example_gyp <- file.readAsLines(root.lib.src.webrtc.`webrtc_examples.gyp`)
      _ <- file.write(root.lib.src.webrtc.`webrtc_examples.gyp`, {
        val lines = original_webrtc_example_gyp
        // line at `'targets': [` - this is right below the predicate text
        val targetsIndex = lines.indexWhere(_.contains("""'OS=="ios" or (OS=="mac" and target_arch!="ia32")'""")) + 1
        lines.updated(targetsIndex, lines(targetsIndex) + """
          { 'target_name': 'WebRTCiOS', # Injected target using github.com/danishin/webrtc-automator
            'type': 'shared_library', # We are creating a dummy shared_library so all the dependencies are built as static libraries. i think this is a bug
            'dependencies': [
              '<(webrtc_root)/system_wrappers/system_wrappers.gyp:field_trial_default',
              '../talk/app/webrtc/legacy_objc_api.gyp:libjingle_peerconnection_objc'
              # '../webrtc/api/api.gyp:rtc_api_objc' # TODO: Use this when API is stabilized
            ],
            'sources': [
            ],
            'export_dependent_settings': [
              '../talk/app/webrtc/legacy_objc_api.gyp:libjingle_peerconnection_objc'
              # '../webrtc/api/api.gyp:rtc_api_objc' # TODO: Use this when API is stabilized
            ],
          },
        """).mkString("\n")
      })

      _ <- putEnv(Env(
        cwd = root.lib.src,
        envVars = Map(
          "GYP_CROSSCOMPILE" -> "1",
          "GYP_GENERATORS" -> "ninja",
          /**
            * - `clang_xcode=1` is needed because chromium project is using old version of clang. Refer to https://bugs.chromium.org/p/webrtc/issues/detail?id=5182
            * - `ios_deployment_target=8.0` is needed because chromium project sets it as 9.0 by default (or whatever is the latest) and if your ios project is less than 9.0, it will generate annoying warnings.
            *   Refer to https://bugs.chromium.org/p/webrtc/issues/detail?id=5563#c6 and https://code.google.com/p/chromium/codesearch#chromium/src/build/common.gypi&l=1687
            */
          "GYP_DEFINES" -> s"OS=ios target_arch=${arch.target_arch} build_with_libjingle=1 build_with_chromium=0 clang_xcode=1 ios_deployment_target=$IOS_DEPLOYMENT_TARGET",
          "GYP_GENERATOR_FLAGS" -> s"output_dir=${arch.output_dir_name}"
        )))

      output_dir = root.lib.src(arch.output_dir_name)
      _ <- shell("rm", "-rf", output_dir)

      _ <- echo("Generate New Build File")
      _ <- shell("python", "webrtc/build/gyp_webrtc")

      output_flavor_dir = s"$output_dir/$buildType-${arch.target}"
      _ <- echo("Start Compiling")
      _ <- shell("ninja", "-C", output_flavor_dir, arch.extra_ninja_build_flag.getOrElse(""), BuildTarget)

      output_flavor_archive_file = s"$output_flavor_dir/${arch.archive_file_name}"
      _ <- echo("Produce Statically Linked Archive File From the Input Files")
      _ <- shell("libtool", "-static", "-o", output_flavor_archive_file, s"$output_flavor_dir/*.a")

      _ <- modifyEnv(_.copy(cwd = "."))
      _ <- shell("mkdir", "-p", root.output.archive)
      archive_file_path = root.output.archive(arch.archive_file_name)
      _ <- shell("rm", "-f", archive_file_path)
      _ <- echo("Strip Archive File of Unnecessary Symbols and Write to output/archive/")
      // -S: Remove the debugging symbol table entries
      // -x: Remove all local symbols (saving only global symbols)
      _ <- shell("strip", "-S", "-x", "-o", archive_file_path, output_flavor_archive_file)

      _ <- echo("Revert back `webrtc/webrtc_examples.gyp` to original")
      _ <- file.write(root.lib.src.webrtc.`webrtc_examples.gyp`, original_webrtc_example_gyp.mkString("\n"))
    } yield ()

    case Platform.Android =>
      ???
  }
}

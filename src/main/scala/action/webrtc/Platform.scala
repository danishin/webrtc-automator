package action.webrtc

sealed trait Platform { self =>
  protected def value: String

  sealed trait Architecture {
    // NB: Using `def` here is sufficient since inner trait already holds reference to its outer trait and store it in `Platform.this`. Using `val` for this will only create an additional reference to it.
    def platform: Platform.this.type = self

    /**
      * Represent the sub-directory of `output_dir` corresponding to the target of the underlying Architecture. (i.e. Simulator or Device)
      * eg. iphonesimulator | iphoneos
      */
    def target: String

    /**
      * target value passed as argument - accordingly will produce single or multiple architecture output file.
      * eg. arm | sim
      */
    protected def target_value: String

    /**
      * Used as value of Environment Variable `GYP_DEFINES='target_arch=?'`
      * eg. ia32 | x64 | arm | arm64
      */
    def target_arch: String

    /**
      * architecture name
      * eg. sim32 | sim64 | armv7 | arm64
      */
    protected def target_arch_value: String

    /**
      * Used as value of Environment Variable `GYP_DEFINES='target_subarch=?'` OVERRIDABLE
      * eg. arm64
      */
    val target_subarch: Option[String] = None

    /**
      * Extra `ninja -C` flag (notably, `iossim` from `IOS.Simulator`) OVERRIDABLE
      */
    val extra_ninja_build_flag: Option[String] = None

    /**
      * Used as value of Environment Variable `GYP_GENERATOR_FLAGS='output_dir=?'`
      */
    final lazy val output_dir_name = s"out_$target_arch_value"

    /**
      * Name of this architecture's output archive file
      */
    final lazy val archive_file_name = s"libWebRTC-${platform.value}-$target_arch_value.a"

    /**
      * Full path of archive file
      */
    def archive_file_path = root.output.archive(archive_file_name)

    override def toString = s"$platform - $target_arch_value"
  }

  protected def allArchs: List[Architecture]

  object Architecture {
    def parse(str: String): Option[List[Architecture]] = str match {
      case "all" => Some(allArchs)
      case _ =>
        // Filter by sim, sim32, sim64, arm, armv7, arm64
        allArchs.filter(a => a.target_value == str || a.target_arch_value == str) match {
          case Nil => None
          case as => Some(as)
        }
    }
  }

  override def toString = value
}

object Platform {
  case object IOS extends Platform {
    protected val value = "ios"

    sealed trait Sim extends Architecture {
      val target = "iphonesimulator"
      protected val target_value = "sim"
      override val extra_ninja_build_flag = Some("iossim")
    }

    // 32bit Simulator (ia32 architecture) upto iPhone5
    case object Sim32 extends Sim {
      val target_arch = "ia32"
      protected val target_arch_value = "sim32"
    }

    // 64bit Simulator (x86_64 architecture) iPhone5S onwards
    case object Sim64 extends Sim {
      val target_arch = "x64"
      protected val target_arch_value = "sim64"
      override val target_subarch = Some("arm64")
    }

    sealed trait ARM extends Architecture {
      val target = "iphoneos"
      protected val target_value = "arm"
    }

    // 32bit Real Device (armv7 architecture) upto iPhone5
    case object ARMv7 extends ARM {
      val target_arch = "arm"
      protected val target_arch_value = "armv7"
    }

    // 64bit Real Device (arm64 architecture) iPhone5S onwards
    case object ARM64 extends ARM {
      val target_arch = "arm64"
      protected val target_arch_value = "arm64"
    }

    protected val allArchs = List(Sim32, Sim64, ARMv7, ARM64)
  }

  case object Android extends Platform {
    protected val value = "android"
    protected val allArchs = List()
  }

  def parse(str: String): Option[Platform] = List(IOS, Android).find(_.value == str)
}

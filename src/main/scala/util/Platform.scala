package util

sealed trait Platform { self =>
  protected def value: String

  sealed trait Architecture {
    // NB: Using `def` here is sufficient since inner trait already holds reference to its outer trait and store it in `Platform.this`. Using `val` for this will only create an additional reference to it.
    def platform: Platform.this.type = self

    /**
      * flavor value passed as argument - accordingly will produce single or multiple architecture output file.
      */
    protected def flavor_value: String

    /**
      * architecture name
      */
    protected def value: String

    /**
      * Used as value of Environment Variable `GYP_DEFINES='target_arch=?'`
      */
    def target_arch: String

    /**
      * Used as value of Environment Variable `GYP_DEFINES='target_subarch=?'` OVERRIDABLE
      */
    val target_subarch: Option[String] = None

    /**
      * Represent the sub-directory of `output_dir` corresponding to the flavor of the underlying Arhictecture.
      */
    def flavor: String

    /**
      * Extra `ninja -C` flag (notably, `iossim` from `IOS.Simulator`) OVERRIDABLE
      */
    val extra_ninja_build_flag: Option[String] = None

    /**
      * Used as value of Environment Variable `GYP_GENERATOR_FLAGS='output_dir=?'`
      */
    final lazy val output_dir_name = s"out_$value"

    /**
      * Name of this architecture's output archive file
      */
    final lazy val archive_file_name = s"libWebRTC-${platform.value}-$value.a"

    override def toString = s"${platform.toString} - $value"
  }

  protected def allArchs: List[Architecture]

  object Architecture {
    def parse(str: String): Option[List[Architecture]] = allArchs.filter(_.flavor_value == str) match {
      case Nil => None
      case as => Some(as)
    }
  }

  override def toString = value
}

object Platform {
  case object IOS extends Platform {
    protected val value = "ios"

    sealed trait Sim extends Architecture {
      protected val flavor_value = "sim"
      val flavor = "Release-iphonesimulator"
      override val extra_ninja_build_flag = Some("iossim")
    }

    // 32bit Simulator (ia32 architecture) upto iPhone5
    case object Sim32 extends Sim {
      protected val value = "sim32"
      val target_arch = "ia32"
    }

    // 64bit Simulator (x86_64 architecture) iPhone5S onwards
    case object Sim64 extends Sim {
      protected val value = "sim64"
      val target_arch = "x64"
      override val target_subarch = Some("arm64")
    }

    sealed trait ARM extends Architecture {
      protected val flavor_value = "arm"
      val flavor = "Release-iphoneos"
    }

    // 32bit Real Device (armv7 architecture) upto iPhone5
    case object ARMv7 extends ARM {
      protected val value = "armv7"
      val target_arch = "arm"
    }

    // 64bit Real Device (arm64 architecture) iPhone5S onwards
    case object ARM64 extends ARM {
      protected val value = "arm64"
      val target_arch = "arm64"
    }

    protected val allArchs = List(Sim32, Sim64, ARMv7, ARM64)
  }

  case object Android extends Platform {
    protected val value = "android"
    protected val allArchs = List()
  }

  def parse(str: String): Option[Platform] = List(IOS, Android).find(_.value == str)
}

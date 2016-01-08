package util

sealed trait Platform {
  protected def value: String

  sealed trait Architecture {
    val platform: Platform.this.type = Platform.this

    /**
      * architecture name
      */
    protected def value: String

    /**
      * Used as value of Environment Variable `GYP_DEFINES='target_arch=?'`
      */
    def target_arch: String

    /**
      * Represent the sub-directory of `output_dir` corresponding to the flavor of the underlying Arhictecture.
      */
    def flavor: String

    /**
      * Extra `ninja -C` flag (notably, `iossim` from `IOS.Simulator`) - should be overrided as needed
      */
    val extra_ninja_build_flag: Option[String] = None

    /**
      * Used as value of Environment Variable `GYP_GENERATOR_FLAGS='output_dir=?'`
      */
    final lazy val output_dir_name = s"out_$value"

    /**
      * Name of this architecture's output archive file
      */
    final lazy val archive_file_name = s"libWebRTC-$value.a"

    override def toString = s"${platform.toString} - $value"
  }

  protected def allArchs: List[Architecture]

  object Architecture {
    def parse(str: String): Option[Architecture] = allArchs.find(_.value == str)
  }

  override def toString = value
}

object Platform {
  case object IOS extends Platform {
    protected val value = "ios"

    case object Simulator extends Architecture {
      protected val value = "sim"
      val target_arch = "ia32"
      val flavor = "Release-iphonesimulator"
      override val extra_ninja_build_flag = Some("iossim")
    }

    case object ARMv7 extends Architecture {
      protected val value = "armv7"
      val target_arch = "arm"
      val flavor = "Release-iphoneos"
    }

    case object ARM64 extends Architecture {
      protected val value = "arm64"
      val target_arch = "arm64"
      val flavor = "Release-iphoneos"
    }

    protected val allArchs = List(Simulator, ARMv7, ARM64)
  }

  case object Android extends Platform {
    protected val value = "android"
    protected val allArchs = List()
  }

  def parse(str: String): Option[Platform] = List(IOS, Android).find(_.value == str)
}

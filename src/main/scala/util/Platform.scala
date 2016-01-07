package util


sealed trait Platform {
  sealed trait Architecture {
    private[util] def value: String
    def target_arch_identifier: String
    val outArchiveFilePath = s"output/tmp/libWebRTC-$value.a"
  }

  protected def allArchs: List[Architecture]

  def parseArch(str: String): Option[Architecture] = allArchs.find(_.value == str)
}

object Platform {
  case object IOS extends Platform {
    case object Sim32 extends Architecture {
      private[util] val value = "sim32"
      val target_arch_identifier = "ia32"
    }

    case object Sim64 extends Architecture {
      private[util] val value = "sim64"
      val target_arch_identifier = "x64"
    }

    case object ARMv7 extends Architecture {
      private[util] val value = "armv7"
      val target_arch_identifier = "arm"
    }

    case object ARM64 extends Architecture {
      private[util] val value = "arm64"
      val target_arch_identifier = "arm64"
    }

    protected val allArchs = List(Sim32, Sim64, ARMv7, ARM64)
  }

  case object Android extends Platform {
    protected val allArchs = List()
  }

  def parse(str: String): Option[Platform] = str match {
    case "ios" => Some(IOS)
    case "android" => Some(Android)
    case _ => None
  }
}

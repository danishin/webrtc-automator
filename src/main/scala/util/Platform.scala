package util

sealed trait Architecture {
  private[util] def value: String
  def target_arch_identifier: String

  val outArchiveFilePath = s"output/tmp/libWebRTC-$value.a"

}

sealed trait Platform {
  type Arch <: Architecture
  protected def allArchs: List[Arch]

  def getArch(str: String): Option[Arch] = allArchs.find(_.value == str)
}

object Platform {
  case object IOS extends Platform {
    sealed trait Arch extends Architecture
    case object Sim32 extends Arch {
      private[util] val value = "sim32"
      val target_arch_identifier = "ia32"
    }

    case object Sim64 extends Arch {
      private[util] val value = "sim64"
      val target_arch_identifier = "x64"
    }

    case object ARMv7 extends Arch {
      private[util] val value = "armv7"
      val target_arch_identifier = "arm"
    }

    case object ARM64 extends Arch {
      private[util] val value = "arm64"
      val target_arch_identifier = "arm64"
    }

    protected val allArchs = List(Sim32, Sim64, ARMv7, ARM64)
  }

  case object Android extends Platform {
    sealed trait Arch extends Architecture
    protected val allArchs = List()
  }

  def from(str: String): Option[Platform] = str match {
    case "ios" => Some(IOS)
    case "android" => Some(Android)
    case _ => None
  }
}

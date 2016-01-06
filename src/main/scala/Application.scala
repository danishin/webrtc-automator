import java.io.File

import build.Build
import fetch.Fetch
import update.Update
import util.{Helper, Platform}

object Application {
  def main(args: Array[String]) {
    val opt: Option[Unit] = args match {
      case Array("fetch", platformStr) => Platform.from(platformStr).map(Fetch.run)
      case Array("update", platformStr) => Platform.from(platformStr).map(Update.run)
      case Array("build", platformStr, archStr) => Platform.from(platformStr).flatMap(p => p.getArch(archStr).map(Build.run(p)))
      case _ => None
    }

    if(opt.isEmpty) Helper.exitError("Invalid Argument")
  }
}

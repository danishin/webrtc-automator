name := "webrtc_build"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val scalazVersion = "7.2.0"
  Seq(
    "org.scalaz" %% "scalaz-core" % scalazVersion
  )
}
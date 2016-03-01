name := "webrtc_automator"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val awsVersion = "1.10.56"
  val scalazVersion = "7.2.0"

  Seq(
    "com.amazonaws" % "aws-java-sdk-core" % awsVersion,
    "com.amazonaws" % "aws-java-sdk-ec2" % awsVersion,

    "com.typesafe.play" %% "play-json" % "2.5.0-M2",
    "com.decodified" %% "scala-ssh" % "0.7.0",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "org.scalaz" %% "scalaz-core" % scalazVersion
  )
}
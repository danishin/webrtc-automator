lazy val `webrtc-automator` = (project in file(".")).enablePlugins(SbtTwirl)
version := "1.0"
scalaVersion := "2.11.7"

libraryDependencies ++= {
  val awsVersion = "1.10.56"
  val scalazVersion = "7.2.0"

  Seq(
    "com.amazonaws" % "aws-java-sdk-core" % awsVersion,
    "com.amazonaws" % "aws-java-sdk-ec2" % awsVersion,

    "com.typesafe.play" %% "play-json" % "2.5.0-M2",
    "org.scalaz" %% "scalaz-core" % scalazVersion,
    "org.scalaj" %% "scalaj-http" % "2.2.1"
  )
}

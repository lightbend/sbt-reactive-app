sbtPlugin := true

val Versions = new {
  val crossSbtVersions = Vector("0.13.16", "1.0.0")
  val nativePackager   = "1.2.2"
  val playJson         = "2.6.5"
  val scala            = "2.12.3"
  val scalaTest        = "3.0.1"
}

name := "sbt-reactive-app"
organization := "com.lightbend.rp"
organizationName := "Lightbend, Inc."
startYear := Some(2017)
licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))

scalaVersion in Global := Versions.scala
crossSbtVersions := Versions.crossSbtVersions
scalacOptions ++= Vector("-deprecation")

libraryDependencies ++= Vector(
  "com.typesafe.play" %% "play-json" % Versions.playJson,
  "org.scalatest"     %% "scalatest" % Versions.scalaTest % "test"
)

enablePlugins(AutomateHeaderPlugin)

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % Versions.nativePackager)

publishMavenStyle := false

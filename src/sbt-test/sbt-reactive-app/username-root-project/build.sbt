version := "0.1.2-SNAPSHOT"
scalaVersion := "2.12.4"

enablePlugins(SbtReactiveAppPlugin)

lazy val `hello-building` = (project in file("."))
  .aggregate(boxes)

lazy val boxes = (project in file("boxes"))
  .enablePlugins(SbtReactiveAppPlugin)

TaskKey[Unit]("check") := {
  val dockerUsernameValue = (dockerUsername in Docker in boxes).value
  val dockerUsernameValueExpected = Some("hello-building")
  assert(dockerUsernameValue == dockerUsernameValueExpected,
    s"Docker repository value is $dockerUsernameValue - expected $dockerUsernameValueExpected}")
}

name := "sans"
version := "0.1.2-SNAPSHOT"
scalaVersion := "2.12.4"

enablePlugins(SbtReactiveAppPlugin)

TaskKey[Unit]("check") := {
  val dockerUsernameValue = (dockerUsername in Docker).value
  val dockerUsernameValueExpected = Some("sans")
  assert(dockerUsernameValue == dockerUsernameValueExpected,
    s"Docker repository value is $dockerUsernameValue - expected $dockerUsernameValueExpected}")
}

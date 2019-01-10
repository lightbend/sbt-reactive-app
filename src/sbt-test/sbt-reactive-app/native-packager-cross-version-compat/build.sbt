name := "hello"
scalaVersion := "2.11.12"

lazy val root = (project in file("."))
  .enablePlugins(SbtReactiveAppPlugin)
  .settings(
    packageName in Docker := "hello-play",
    httpIngressPorts := Seq(9000),
    httpIngressPaths := Seq("/")
  )


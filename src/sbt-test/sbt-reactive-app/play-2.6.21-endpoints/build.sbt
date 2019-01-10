name := "hello-play"
scalaVersion := "2.11.12"

libraryDependencies += guice

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtReactiveAppPlugin)
  .settings(
    packageName in Docker := "hello-play",
    httpIngressPorts := Seq(9000),
    httpIngressPaths := Seq("/")
  )


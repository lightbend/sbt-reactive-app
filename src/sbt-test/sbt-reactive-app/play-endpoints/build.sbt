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

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """LABEL com.lightbend.rp.endpoints.0.protocol="http"""",
    """LABEL com.lightbend.rp.endpoints.0.ingress.0.ingress-ports.0="9000"""",
    """LABEL com.lightbend.rp.endpoints.0.ingress.0.type="http"""",
    """LABEL com.lightbend.rp.endpoints.0.ingress.0.paths.0="/"""",
    """LABEL com.lightbend.rp.endpoints.0.name="http"""",
    """LABEL com.lightbend.rp.modules.akka-cluster-bootstrapping.enabled="false"""",
    """LABEL com.lightbend.rp.modules.play-http-binding.enabled="true"""",
    """LABEL com.lightbend.rp.app-type="play"""",
    """LABEL com.lightbend.rp.app-name="hello-play"""",
    """LABEL com.lightbend.rp.modules.common.enabled="true"""",
    """LABEL com.lightbend.rp.modules.secrets.enabled="false"""",
    """LABEL com.lightbend.rp.modules.service-discovery.enabled="false""""
  )

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(s"""Dockerfile is missing line "$line"""")
    }
  }
}
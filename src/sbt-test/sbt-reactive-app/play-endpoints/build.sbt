name := "hello-play"
scalaVersion := "2.11.12"

libraryDependencies += guice

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtReactiveAppPlugin)
  .settings(
    packageName in Docker := "hello-play",
    rpHttpIngressPorts := Seq(9000),
    rpHttpIngressPaths := Seq("/")
  )

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.read(outputDir / "Dockerfile")
  val lines = Seq(
    """com.lightbend.rp.endpoints.0.protocol="http"""",
    """com.lightbend.rp.endpoints.0.ingress.0.ingress-ports.0="9000"""",
    """com.lightbend.rp.endpoints.0.ingress.0.type="http"""",
    """com.lightbend.rp.endpoints.0.ingress.0.paths.0="/"""",
    """com.lightbend.rp.endpoints.0.name="http"""",
    """com.lightbend.rp.modules.akka-cluster-bootstrapping.enabled="false"""",
    """com.lightbend.rp.modules.play-http-binding.enabled="true"""",
    """com.lightbend.rp.app-type="play"""",
    """com.lightbend.rp.app-name="hello-play"""",
    """com.lightbend.rp.modules.common.enabled="true"""",
    """com.lightbend.rp.modules.secrets.enabled="false"""",
    """com.lightbend.rp.modules.service-discovery.enabled="false""""
  )

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(s"""Dockerfile is missing line "$line"""")
    }
  }
}

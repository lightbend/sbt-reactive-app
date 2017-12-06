name := "hello-akka"
scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.6"

lazy val root = (project in file("."))
  .enablePlugins(SbtReactiveAppPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion
    ),

    packageName in Docker := "hello-akka",
    enableAkkaClusterBootstrap := Some(true)
  )

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """ENTRYPOINT ["/rp-start", "bin/hello-akka"]""",
    """COPY rp-start /rp-start""",
    """LABEL com.lightbend.rp.endpoints.0.protocol="tcp"""",
    """LABEL com.lightbend.rp.endpoints.0.name="akka-remote"""",
    """LABEL com.lightbend.rp.endpoints.1.protocol="tcp"""",
    """LABEL com.lightbend.rp.endpoints.1.name="akka-mgmt-http"""",
    """LABEL com.lightbend.rp.modules.akka-cluster-bootstrapping.enabled="true"""",
    """LABEL com.lightbend.rp.modules.play-http-binding.enabled="false"""",
    """LABEL com.lightbend.rp.app-type="basic"""",
    """LABEL com.lightbend.rp.app-name="hello-akka"""",
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
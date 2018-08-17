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
    rpEnableAkkaClusterBootstrap := true,
    rpAkkaClusterBootstrapSystemName := "ClusterSystem"
  )

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.read(outputDir / "Dockerfile")
  val lines = Seq(
    """com.lightbend.rp.endpoints.0.protocol="tcp"""",
    """com.lightbend.rp.endpoints.0.name="akka-remote"""",
    """com.lightbend.rp.endpoints.1.protocol="tcp"""",
    """com.lightbend.rp.endpoints.1.name="akka-mgmt-http"""",
    """com.lightbend.rp.modules.akka-cluster-bootstrapping.enabled="true"""",
    """com.lightbend.rp.modules.play-http-binding.enabled="false"""",
    """com.lightbend.rp.app-type="basic"""",
    """com.lightbend.rp.app-name="hello-akka"""",
    """com.lightbend.rp.modules.common.enabled="true"""",
    """com.lightbend.rp.modules.secrets.enabled="false"""",
    """com.lightbend.rp.modules.service-discovery.enabled="true"""",
    """com.lightbend.rp.akka-cluster-bootstrap.system-name="ClusterSystem""""
  )

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(
        s"""|Dockerfile is missing line "$line" - Dockerfile contents:
            |${contents.mkString("\n")}
            |""".stripMargin)
    }
  }
}

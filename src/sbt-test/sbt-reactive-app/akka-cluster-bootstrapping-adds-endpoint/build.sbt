name := "akka-cluster-bootstrapping-adds-endpoint"

enablePlugins(SbtReactiveAppPlugin)

enableAkkaClusterBootstrap := Some(true)

akkaClusterBootstrapEndpointName := "my-akka-remote"

TaskKey[Unit]("check") := {
  assert(endpoints.value.contains(TcpEndpoint("my-akka-remote", 0)))

  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """LABEL com.lightbend.rp.app-type="basic"""",
    """LABEL com.lightbend.rp.modules.akka-cluster-bootstrapping.enabled="true"""",
    """LABEL com.lightbend.rp.modules.common.enabled="true"""",
    """LABEL com.lightbend.rp.modules.play-http-binding.enabled="false"""",
    """LABEL com.lightbend.rp.modules.secrets.enabled="false""""
  )

  println(contents)

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(s"""Dockerfile is missing line "$line"""")
    }
  }
}

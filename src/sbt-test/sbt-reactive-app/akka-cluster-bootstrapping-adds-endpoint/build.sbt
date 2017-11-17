name := "akka-cluster-bootstrapping-adds-endpoint"

enablePlugins(SbtReactiveAppPlugin)

enableAkkaClusterBootstrap := Some(true)

akkaClusterBootstrapEndpointName := "my-akka-remote"

TaskKey[Unit]("check") := {
  assert(endpoints.value.contains(TcpEndpoint("my-akka-remote", 0)))
}

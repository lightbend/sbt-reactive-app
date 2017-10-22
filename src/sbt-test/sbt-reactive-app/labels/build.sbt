version := "0.1.2-SNAPSHOT"
scalaVersion := "2.12.3"

enablePlugins(DockerPlugin)

nrOfCpus := Some(0.5)
memory := Some(65536)
diskSpace := Some(32768)
privileged := true
volumes := Map("/data" -> HostPathVolume("/var/local"), "/data2" -> SecretVolume("my-secret"))
endpoints := Map("test1" -> Endpoint("http", 2551, HttpAcl("^/test.*$"), TcpAcl(8080)))
environmentVariables := Map(
  "APPLICATION_SECRET" -> SecretEnvironmentVariable("my-app-secret"),
  "HOME" -> LiteralEnvironmentVariable("/home/testing"))
healthCheck := Some(CommandCheck("/bin/bash", "-c", "exit 0"))
readinessCheck := Some(HttpCheck(1234, 60, "/healthz"))

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """LABEL com.lightbend.rp.disk-space="32768"""",
    """LABEL com.lightbend.rp.endpoints.0.acls.0.expression="^/test.*$"""",
    """LABEL com.lightbend.rp.endpoints.0.acls.0.type="http"""",
    """LABEL com.lightbend.rp.endpoints.0.acls.1.ports.0="8080"""",
    """LABEL com.lightbend.rp.endpoints.0.acls.1.type="tcp"""",
    """LABEL com.lightbend.rp.endpoints.0.name="test1"""",
    """LABEL com.lightbend.rp.endpoints.0.port="2551"""",
    """LABEL com.lightbend.rp.endpoints.0.protocol="http"""",
    """LABEL com.lightbend.rp.environment-variables.0.name="APPLICATION_SECRET"""",
    """LABEL com.lightbend.rp.environment-variables.0.secret="my-app-secret"""",
    """LABEL com.lightbend.rp.environment-variables.0.type="secret"""",
    """LABEL com.lightbend.rp.environment-variables.1.name="HOME"""",
    """LABEL com.lightbend.rp.environment-variables.1.type="literal"""",
    """LABEL com.lightbend.rp.environment-variables.1.value="/home/testing"""",
    """LABEL com.lightbend.rp.health-check.args.0="/bin/bash"""",
    """LABEL com.lightbend.rp.health-check.args.1="-c"""",
    """LABEL com.lightbend.rp.health-check.args.2="exit 0"""",
    """LABEL com.lightbend.rp.health-check.type="command"""",
    """LABEL com.lightbend.rp.memory="65536"""",
    """LABEL com.lightbend.rp.nr-of-cpus="0.5"""",
    """LABEL com.lightbend.rp.privileged="true"""",
    """LABEL com.lightbend.rp.readiness-check.interval="60"""",
    """LABEL com.lightbend.rp.readiness-check.path="/healthz"""",
    """LABEL com.lightbend.rp.readiness-check.port="1234"""",
    """LABEL com.lightbend.rp.readiness-check.type="http"""",
    """LABEL com.lightbend.rp.volumes.0.guest-path="/data"""",
    """LABEL com.lightbend.rp.volumes.0.path="/var/local"""",
    """LABEL com.lightbend.rp.volumes.0.type="host-path"""",
    """LABEL com.lightbend.rp.volumes.1.guest-path="/data2"""",
    """LABEL com.lightbend.rp.volumes.1.secret="my-secret"""",
    """LABEL com.lightbend.rp.volumes.1.type="secret"""",
    """LABEL com.lightbend.rp.version-major="0"""",
    """LABEL com.lightbend.rp.version-minor="1"""",
    """LABEL com.lightbend.rp.version-patch="2"""",
    """LABEL com.lightbend.rp.version-patch-label="SNAPSHOT"""")

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(s"""Dockerfile is missing line "$line"""")
    }
  }
}

name := "labels"
version := "0.1.2-SNAPSHOT"
scalaVersion := "2.12.4"

enablePlugins(SbtReactiveAppPlugin)

namespace := Some("fonts")
nrOfCpus := Some(0.5)
memory := Some(65536)
diskSpace := Some(32768)
privileged := true
volumes := Map("/data" -> HostPathVolume("/var/local"), "/data2" -> HostPathVolume("/var/log"))
endpoints := Vector(HttpEndpoint("test1", 2551, HttpIngress(ingressPorts = Seq(80, 443), hosts = Seq("hi.com"), paths = Seq("/test.*$"))))
environmentVariables := Map(
  "LD_LIBRARY_PATH" -> LiteralEnvironmentVariable("/lib"),
  "HOME" -> LiteralEnvironmentVariable("/home/testing"))
secrets := Set(Secret("myns1", "key"), Secret("myns2", "otherkey"))
healthCheck := Some(CommandCheck("/bin/bash", "-c", "exit 0"))
readinessCheck := Some(HttpCheck(1234, 60, "/healthz"))

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """COPY rp-start /rp-start""",
    """ENTRYPOINT ["/rp-start", "bin/labels"]""",
    """LABEL com.lightbend.rp.namespace="fonts"""",
    """LABEL com.lightbend.rp.app-name="labels"""",
    """LABEL com.lightbend.rp.disk-space="32768"""",
    """LABEL com.lightbend.rp.endpoints.0.ingress.0.type="http"""",
    """LABEL com.lightbend.rp.endpoints.0.ingress.0.ingress-ports.0="80"""",
    """LABEL com.lightbend.rp.endpoints.0.ingress.0.ingress-ports.1="443"""",
    """LABEL com.lightbend.rp.endpoints.0.ingress.0.paths.0="/test.*$"""",
    """LABEL com.lightbend.rp.endpoints.0.ingress.0.hosts.0="hi.com"""",
    """LABEL com.lightbend.rp.endpoints.0.name="test1"""",
    """LABEL com.lightbend.rp.endpoints.0.port="2551"""",
    """LABEL com.lightbend.rp.endpoints.0.protocol="http"""",
    """LABEL com.lightbend.rp.environment-variables.0.name="LD_LIBRARY_PATH"""",
    """LABEL com.lightbend.rp.environment-variables.0.value="/lib"""",
    """LABEL com.lightbend.rp.environment-variables.0.type="literal"""",
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
    """LABEL com.lightbend.rp.volumes.1.path="/var/log"""",
    """LABEL com.lightbend.rp.volumes.1.type="host-path"""",
    """LABEL com.lightbend.rp.version-major="0"""",
    """LABEL com.lightbend.rp.version-minor="1"""",
    """LABEL com.lightbend.rp.version-patch="2"""",
    """LABEL com.lightbend.rp.version-patch-label="SNAPSHOT"""",
    """LABEL com.lightbend.rp.secrets.0.namespace="myns1"""",
    """LABEL com.lightbend.rp.secrets.0.name="key"""",
    """LABEL com.lightbend.rp.secrets.1.namespace="myns2"""",
    """LABEL com.lightbend.rp.secrets.1.name="otherkey"""",
    """LABEL com.lightbend.rp.app-type="basic"""",
    """LABEL com.lightbend.rp.modules.akka-cluster-bootstrapping.enabled="false"""",
    """LABEL com.lightbend.rp.modules.common.enabled="true"""",
    """LABEL com.lightbend.rp.modules.play-http-binding.enabled="false"""",
    """LABEL com.lightbend.rp.modules.secrets.enabled="true""""
  )

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(
        s"""Dockerfile is missing line "$line" - Dockerfile contents:
           |${contents.mkString("\n")}
         """.stripMargin)
    }
  }

  assert(
    (dockerBaseImage in Docker).value == "openjdk:8-jre-alpine" || true,
    "Docker image incorrectly set")

  val dockerUsernameValue = (dockerUsername in Docker).value
  val dockerUsernameValueExpected = Some("fonts")
  assert(dockerUsernameValue == dockerUsernameValueExpected,
    s"Docker repository value is $dockerUsernameValue - expected $dockerUsernameValueExpected}")
}

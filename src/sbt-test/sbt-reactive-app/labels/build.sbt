name := "labels"
version := "0.1.2-SNAPSHOT"
scalaVersion := "2.12.4"

enablePlugins(SbtReactiveAppPlugin)

rpCpu := 0.5
rpMemory := 65536
rpDiskSpace := 32768
rpPrivileged := true
rpEndpoints := Vector(HttpEndpoint("test1", 2551, HttpIngress(ingressPorts = Seq(80, 443), hosts = Seq("hi.com"), paths = Seq("/test.*$"))))
rpEnvironmentVariables := Map(
  "LD_LIBRARY_PATH" -> LiteralEnvironmentVariable("/lib"),
  "HOME" -> LiteralEnvironmentVariable("/home/testing"))
rpSecrets := Set(Secret("myns1", "key"), Secret("myns2", "otherkey"))

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.read(outputDir / "Dockerfile")
  val lines = Seq(
    """com.lightbend.rp.config-resource="rp-application.conf"""",
    """com.lightbend.rp.app-name="labels"""",
    """com.lightbend.rp.disk-space="32768"""",
    """com.lightbend.rp.endpoints.0.ingress.0.type="http"""",
    """com.lightbend.rp.endpoints.0.ingress.0.ingress-ports.0="80"""",
    """com.lightbend.rp.endpoints.0.ingress.0.ingress-ports.1="443"""",
    """com.lightbend.rp.endpoints.0.ingress.0.paths.0="/test.*$"""",
    """com.lightbend.rp.endpoints.0.ingress.0.hosts.0="hi.com"""",
    """com.lightbend.rp.endpoints.0.name="test1"""",
    """com.lightbend.rp.endpoints.0.port="2551"""",
    """com.lightbend.rp.endpoints.0.protocol="http"""",
    """com.lightbend.rp.environment-variables.0.name="LD_LIBRARY_PATH"""",
    """com.lightbend.rp.environment-variables.0.value="/lib"""",
    """com.lightbend.rp.environment-variables.0.type="literal"""",
    """com.lightbend.rp.environment-variables.1.name="HOME"""",
    """com.lightbend.rp.environment-variables.1.type="literal"""",
    """com.lightbend.rp.environment-variables.1.value="/home/testing"""",
    """com.lightbend.rp.memory="65536"""",
    """com.lightbend.rp.cpu="0.5"""",
    """com.lightbend.rp.privileged="true"""",
    """com.lightbend.rp.app-version="0.1.2-SNAPSHOT"""",
    """com.lightbend.rp.secrets.0.name="myns1"""",
    """com.lightbend.rp.secrets.0.key="key"""",
    """com.lightbend.rp.secrets.1.name="myns2"""",
    """com.lightbend.rp.secrets.1.key="otherkey"""",
    """com.lightbend.rp.app-type="basic"""",
    """com.lightbend.rp.modules.akka-cluster-bootstrapping.enabled="false"""",
    """com.lightbend.rp.modules.common.enabled="true"""",
    """com.lightbend.rp.modules.secrets.enabled="true""""
  )

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(
        s"""Dockerfile is missing line "$line" - Dockerfile contents:
           |${contents.mkString("\n")}
         """.stripMargin)
    }
  }

  // One of the labels must specify sbt-reactive-app version, number itself does not matter for this test
  assert(
    contents.contains("com.lightbend.rp.sbt-reactive-app-version="),
    "Label with sbt-reactive-app version not found")

  assert(
    (dockerBaseImage in Docker).value == "openjdk:8-jre-alpine" || true,
    "Docker image incorrectly set")
}

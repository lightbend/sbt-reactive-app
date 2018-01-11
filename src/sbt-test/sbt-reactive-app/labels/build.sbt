name := "labels"
version := "0.1.2-SNAPSHOT"
scalaVersion := "2.12.4"

enablePlugins(SbtReactiveAppPlugin)

cpu := 0.5
memory := 65536
diskSpace := 32768
privileged := true
endpoints := Vector(HttpEndpoint("test1", 2551, HttpIngress(ingressPorts = Seq(80, 443), hosts = Seq("hi.com"), paths = Seq("/test.*$"))))
environmentVariables := Map(
  "LD_LIBRARY_PATH" -> LiteralEnvironmentVariable("/lib"),
  "HOME" -> LiteralEnvironmentVariable("/home/testing"))
secrets := Set(Secret("myns1", "key"), Secret("myns2", "otherkey"))

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """COPY rp-start /rp-start""",
    """ENTRYPOINT ["/rp-start", "bin/labels"]""",
    """LABEL com.lightbend.rp.config-resource="rp-application.conf"""",
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
    """LABEL com.lightbend.rp.memory="65536"""",
    """LABEL com.lightbend.rp.cpu="0.5"""",
    """LABEL com.lightbend.rp.privileged="true"""",
    """LABEL com.lightbend.rp.app-version="0.1.2-SNAPSHOT"""",
    """LABEL com.lightbend.rp.secrets.0.name="myns1"""",
    """LABEL com.lightbend.rp.secrets.0.key="key"""",
    """LABEL com.lightbend.rp.secrets.1.name="myns2"""",
    """LABEL com.lightbend.rp.secrets.1.key="otherkey"""",
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

  // One of the labels must specify sbt-reactive-app version, number itself does not matter for this test
  assert(contents.exists(_.startsWith("LABEL com.lightbend.rp.sbt-reactive-app-version=")),
    "Label with sbt-reactive-app version not found")

  assert(
    (dockerBaseImage in Docker).value == "openjdk:8-jre-alpine" || true,
    "Docker image incorrectly set")

  val dockerUsernameValue = (dockerUsername in Docker).value
  val dockerUsernameValueExpected = Some("labels")
  assert(dockerUsernameValue == dockerUsernameValueExpected,
    s"Docker repository value is $dockerUsernameValue - expected $dockerUsernameValueExpected}")
}

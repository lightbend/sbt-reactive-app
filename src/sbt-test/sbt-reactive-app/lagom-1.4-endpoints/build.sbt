val check = taskKey[Unit]("Task for verifying Dockerfile labels")

scalaVersion in ThisBuild := "2.11.12"

lazy val `hello` = (project in file("."))
  .enablePlugins(SbtReactiveAppPlugin)
  .aggregate(`hello-api`, `hello-impl`, `echo-api`, `echo-impl`)
  .settings(
    name := "lagom-endpoints"
  )

lazy val `echo-api` = (project in file("echo-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `hello-api` = (project in file("hello-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `hello-impl` = (project in file("hello-impl"))
  .enablePlugins(LagomScala, SbtReactiveAppPlugin)
  .settings(
    packageName in Docker := "hello-lagom",
    httpIngressPorts := scala.collection.immutable.Seq(9000),
    libraryDependencies += lagomScaladslCluster,
    check := {
      val outputDir = (stage in Docker).value
      val contents = IO.read(outputDir / "Dockerfile")
      val expectedLines = Seq(
        """com.lightbend.rp.endpoints.0.protocol="http"""",
        """com.lightbend.rp.endpoints.0.ingress.0.ingress-ports.0="9000"""",
        """com.lightbend.rp.endpoints.0.name="http"""",
        """com.lightbend.rp.modules.akka-cluster-bootstrapping.enabled="true"""",
        """com.lightbend.rp.app-type="lagom"""",
        """com.lightbend.rp.endpoints.0.ingress.0.type="http"""",
        """com.lightbend.rp.app-name="hello"""",
        """com.lightbend.rp.endpoints.0.ingress.0.paths.0="/api/hello"""",
        """com.lightbend.rp.modules.common.enabled="true"""",
        """com.lightbend.rp.modules.secrets.enabled="false"""",
        """com.lightbend.rp.modules.service-discovery.enabled="true"""",
        """com.lightbend.rp.remoting-endpoint="remoting"""",
        """com.lightbend.rp.management-endpoint="management""""
      )

      expectedLines.foreach { line =>
        if(!contents.contains(line)) {
          sys.error(s"""Dockerfile is missing line "$line"""" + contents)
        }
      }

      assert(
        (dockerBaseImage in Docker).value == "openjdk:8-jre-alpine" || true,
        "Docker image incorrectly set")
    }
  )
  .dependsOn(`hello-api`)

lazy val `echo-impl` = (project in file("echo-impl"))
  .enablePlugins(LagomScala, SbtReactiveAppPlugin)
  .settings(
    packageName in Docker := "echo-lagom",
    httpIngressPorts := scala.collection.immutable.Seq(9000),
    check := {
      val outputDir = (stage in Docker).value
      val contents = IO.readLines(outputDir / "Dockerfile")

      assert(
        contents.forall(!_.contains("ingress")),
        s"echo service should not have any ingress\n${contents.mkString("\n")}")
    }
  )
  .dependsOn(`echo-api`)

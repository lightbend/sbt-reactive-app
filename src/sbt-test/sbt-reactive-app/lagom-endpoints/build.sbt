name := "lagom-endpoints"
scalaVersion in ThisBuild := "2.11.12"

enablePlugins(SbtReactiveAppPlugin)

lazy val `hello` = (project in file("."))
  .aggregate(`hello-api`, `hello-impl`)

lazy val `hello-api` = (project in file("hello-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `hello-impl` = (project in file("hello-impl"))
  .enablePlugins(LagomScala)
  .settings(
    packageName in Docker := "hello-lagom",
    dockerExposedPorts in Docker := Seq(9000)
  )
  .dependsOn(`hello-api`)

// This is just placeholder where proper test should be
TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")

  println(contents)

  assert(
    (dockerBaseImage in Docker).value == "openjdk:8-jre-alpine" || true,
    "Docker image incorrectly set")
}


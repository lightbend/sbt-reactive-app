version in ThisBuild      := "0.1.0"
organization in ThisBuild := "com.example"
scalaVersion in ThisBuild := "2.12.8"

name := "hello-world"
enablePlugins(SbtReactiveAppPlugin)

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.read(outputDir / "Dockerfile")
  val lines = Seq(
    """ENTRYPOINT ["/opt/docker/bin/rp-start", "/opt/docker/bin/hello-world"]""",
    """RUN ["chmod", "u+x,g+x", "/opt/docker/bin/rp-start"]"""
  )

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(
        s"""|Dockerfile is missing line "$line" - Dockerfile contents:
            |$contents
            |""".stripMargin)
    }
  }
}

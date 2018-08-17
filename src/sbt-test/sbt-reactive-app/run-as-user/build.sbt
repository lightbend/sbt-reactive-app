name := "run-as-user"
scalaVersion := "2.12.4"
enablePlugins(SbtReactiveAppPlugin)

rpRunAsUser := "rpuser"

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """RUN id -g rpuser || addgroup rpuser""",
    """RUN id -u rpuser || adduser rpuser rpuser""",
    """COPY --chown=rpuser:rpuser rp-start /rp-start""",
    """USER rpuser"""
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

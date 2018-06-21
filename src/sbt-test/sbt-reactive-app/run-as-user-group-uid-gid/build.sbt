name := "run-as-use-group-uid-gid"
scalaVersion := "2.12.4"
enablePlugins(SbtReactiveAppPlugin)

runAsUser := "rpuser"
runAsUserGroup := "rpgroup"
runAsUserUID := 50
runAsUserGID := 60

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """RUN id -g rpgroup || addgroup -g 60 rpgroup""",
    """RUN id -u rpuser || adduser -u 50 rpuser rpgroup""",
    """COPY --chown=rpuser:rpgroup rp-start /rp-start""",
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

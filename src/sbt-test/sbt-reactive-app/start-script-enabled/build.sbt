name := "start-script-enabled"

enablePlugins(SbtReactiveAppPlugin)

startScriptLocation := Some("/my-rp-entry")

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """ENTRYPOINT ["/my-rp-entry", "bin/start-script-enabled"]""")

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(s"""Dockerfile is missing line "$line"""")
    }
  }
}

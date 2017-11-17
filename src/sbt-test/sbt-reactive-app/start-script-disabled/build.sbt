name := "start-script-disabled"
scalaVersion := "2.12.4"
enablePlugins(SbtReactiveAppPlugin)

startScriptLocation := None

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """ENTRYPOINT ["bin/start-script-disabled"]""")

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(s"""Dockerfile is missing line "$line"""")
    }
  }
}

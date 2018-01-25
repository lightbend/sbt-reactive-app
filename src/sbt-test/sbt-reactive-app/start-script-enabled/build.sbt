name := "start-script-enabled"
scalaVersion := "2.12.4"
enablePlugins(SbtReactiveAppPlugin)

startScriptLocation := "/my-rp-entry"

applications += "cleanup" -> Seq("bin/test")

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """COPY rp-start /my-rp-entry""",
    """ENTRYPOINT []""",
    """LABEL com.lightbend.rp.applications.0.name="default"""",
    """LABEL com.lightbend.rp.applications.0.arguments.0="/my-rp-entry"""",
    """LABEL com.lightbend.rp.applications.0.arguments.1="bin/start-script-enabled"""",
    """LABEL com.lightbend.rp.applications.1.name="cleanup"""",
    """LABEL com.lightbend.rp.applications.1.arguments.0="/my-rp-entry"""",
    """LABEL com.lightbend.rp.applications.1.arguments.1="bin/test"""")

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(
        s"""|Dockerfile is missing line "$line" - Dockerfile contents:
            |${contents.mkString("\n")}
            |""".stripMargin)
    }
  }
}

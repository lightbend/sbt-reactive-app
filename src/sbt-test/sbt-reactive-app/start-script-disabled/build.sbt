name := "start-script-disabled"
scalaVersion := "2.12.4"
enablePlugins(SbtReactiveAppPlugin)

rpStartScriptLocation := ""

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.read(outputDir / "Dockerfile")
  val lines = Seq(
    """ENTRYPOINT []""",
    """com.lightbend.rp.applications.0.name="default"""",
    """com.lightbend.rp.applications.0.arguments.0="bin/start-script-disabled"""")

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(
        s"""|Dockerfile is missing line "$line" - Dockerfile contents:
            |${contents.mkString("\n")}
            |""".stripMargin)
    }
  }
}

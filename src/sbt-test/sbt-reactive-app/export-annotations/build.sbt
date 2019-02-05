name := "export-annotations"
scalaVersion := "2.12.4"
enablePlugins(SbtReactiveAppPlugin)

rpAnnotations := Map("es.lightbend.com/scrape" -> "true",
                   "es.lightbend.com/port" -> "metrics",
                   "es.lightbend.com/path" -> "/metrics")

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """com.lightbend.rp.annotations.0.key="es.lightbend.com/scrape" \""",
    """com.lightbend.rp.annotations.0.value="true" \""",
    """com.lightbend.rp.annotations.1.key="es.lightbend.com/port" \""",
    """com.lightbend.rp.annotations.1.value="metrics" \""",
    """com.lightbend.rp.annotations.2.key="es.lightbend.com/path" \""",
    """com.lightbend.rp.annotations.2.value="/metrics" \"""
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

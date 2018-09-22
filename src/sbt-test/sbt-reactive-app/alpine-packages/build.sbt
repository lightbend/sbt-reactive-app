name := "alpine-packages"
scalaVersion := "2.12.4"
enablePlugins(SbtReactiveAppPlugin)

alpinePackages += "coreutils"

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  val contents = IO.readLines(outputDir / "Dockerfile")
  val lines = Seq(
    """RUN /sbin/apk add --no-cache coreutils bash""")

  lines.foreach { line =>
    if (!contents.contains(line)) {
      sys.error(
        s"""|Dockerfile is missing line "$line" - Dockerfile contents:
            |${contents.mkString("\n")}
            |""".stripMargin)
    }
  }
}

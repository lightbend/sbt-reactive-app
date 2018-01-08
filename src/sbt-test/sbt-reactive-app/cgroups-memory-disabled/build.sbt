import java.io.FileNotFoundException

name := "cgroups-memory-disabled"
scalaVersion := "2.11.11"

enablePlugins(SbtReactiveAppPlugin)

memory := 1048576
enableCGroupMemoryLimit := false

TaskKey[Unit]("check") := {
  val outputDir = (stage in Docker).value
  try {
    val contents = IO.readLines(outputDir / "opt" / "docker" / "conf" / "application.ini")
    sys.error("application.ini file was found when shouldn't be there")
  } catch {
      case _ : FileNotFoundException => {}
  }
}

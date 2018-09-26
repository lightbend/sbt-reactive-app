import com.typesafe.sbt.packager.docker._

lazy val root = (project in file("."))
  .enablePlugins(SbtReactiveAppPlugin)
  .settings(
    name := "redhat-packages",
    scalaVersion := "2.12.4",
    dockerBaseImage := "redhat-openjdk-18/openjdk18-openshift",
    rpPackagingDockerCommmands := Vector(
      Cmd("RUN", "/usr/bin/yum", "install", "-y", "nodejs")),

    TaskKey[Unit]("check") := {
      val outputDir = (stage in Docker).value
      val contents = IO.readLines(outputDir / "Dockerfile")
      val lines = Seq(
        """FROM redhat-openjdk-18/openjdk18-openshift""",
        """RUN /usr/bin/yum install -y nodejs""")

      lines.foreach { line =>
        if (!contents.contains(line)) {
          sys.error(
            s"""|Dockerfile is missing line "$line" - Dockerfile contents:
                |${contents.mkString("\n")}
                |""".stripMargin)
        }
      }
    }
  )

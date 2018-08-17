name := "prepend-rp-conf"
scalaVersion := "2.12.4"
enablePlugins(SbtReactiveAppPlugin)

rpPrependRpConf := "application.conf"

// Add a few dependencies that contain rp-tooling.conf.
libraryDependencies += "com.lightbend.rp" % "reactive-lib-status_2.12" % "0.7.0"
libraryDependencies += "com.lightbend.rp" % "reactive-lib-akka-management_2.12" % "0.7.0"
libraryDependencies += "com.lightbend.rp" % "reactive-lib-service-discovery_2.12" % "0.7.0"

TaskKey[Unit]("check") := {
  val rpApplicationConf = resourceManaged.value / "main" / "sbt-reactive-app" / "rp-application.conf"
  val contents = IO.read(rpApplicationConf)
  val expectedLines = Seq(
    "# jar:file:.*/com.lightbend.rp/reactive-lib-status_2.12/jars/reactive-lib-status_2.12-0.7.0.jar!/rp-tooling.conf",
    "# jar:file:.*/com.lightbend.rp/reactive-lib-akka-management_2.12/jars/reactive-lib-akka-management_2.12-0.7.0.jar!/rp-tooling.conf",
    "# jar:file:.*/com.lightbend.rp/reactive-lib-service-discovery_2.12/jars/reactive-lib-service-discovery_2.12-0.7.0.jar!/rp-tooling.conf",
    "application.settings.flag = true"
  )
  // Use a single concatenated regex to ensure the order is correct.
  val expectedContents = ("(?s)" + expectedLines.mkString(".*")).r

  expectedContents findFirstIn contents match {
    case None => sys.error(
      s"""|rp-application.conf doesn't match regex:
          |$expectedContents
          |rp-application.conf contents:
          |$contents
          |""".stripMargin
    )
    case Some(_) => sLog.value.warn("Nice job!")
  }
}

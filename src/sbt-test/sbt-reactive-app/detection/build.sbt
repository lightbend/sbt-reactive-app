// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.11.8"

lagomCassandraEnabled in ThisBuild := false
lagomKafkaEnabled in ThisBuild := false

lazy val `detection` = (project in file("."))
  .aggregate(
    `simple-app`,
    frontend,
    `lagom-scala-api`,
    `lagom-scala-impl`,
    `lagom-java-api`,
    `lagom-java-impl`
  )

lazy val `simple-app` = (project in file("simple-app"))
  .enablePlugins(SbtReactiveAppPlugin)

lazy val frontend = (project in file("frontend"))
  .enablePlugins(PlayScala, LagomPlay, SbtReactiveAppPlugin)
  .settings(
    libraryDependencies ++= Seq(
      guice, // This is required to configure Play's application loader
      ws
    )
  )

lazy val `frontend-play` = (project in file("frontend-play"))
  .enablePlugins(PlayScala, SbtReactiveAppPlugin)
  .settings(
    enableServiceDiscovery := true,
    libraryDependencies ++= Seq(
      guice, // This is required to configure Play's application loader
      ws
    )
  )

lazy val `lagom-scala-api` = (project in file("lagom-scala-api"))
  .settings(
    libraryDependencies += lagomScaladslApi
  )

lazy val `lagom-scala-impl` = (project in file("lagom-scala-impl"))
  .enablePlugins(LagomScala, SbtReactiveAppPlugin)
  .settings(
    libraryDependencies += guice // This is required to configure Play's application loader
  )
  .dependsOn(`lagom-scala-api`)

lazy val `lagom-java-api` = (project in file("lagom-java-api"))
  .settings(
    libraryDependencies += lagomJavadslApi
  )

lazy val `lagom-java-impl` = (project in file("lagom-java-impl"))
  .enablePlugins(LagomJava, SbtReactiveAppPlugin)
  .settings(
    libraryDependencies += guice // This is required to configure Play's application loader
  )
  .dependsOn(`lagom-java-api`)


TaskKey[Unit]("checkAppType") := {
  assert(
    (appType in `simple-app`).value == "basic",
    s"Incorrect appType for ${(name in `simple-app`).value}: ${(appType in `simple-app`).value}"
  )

  assert(
    (appType in `frontend-play`).value == "play",
    s"Incorrect appType for ${(name in `frontend-play`).value}: ${(appType in `frontend-play`).value}"
  )

  assert(
    (appType in frontend).value == "lagom",
    s"Incorrect appType for ${(name in frontend).value}: ${(appType in frontend).value}"
  )

  assert(
    (appType in `lagom-java-impl`).value == "lagom",
    s"Incorrect appType for ${(name in `lagom-java-impl`).value}: ${(appType in `lagom-java-impl`).value}"
  )

  assert(
    (appType in `lagom-scala-impl`).value == "lagom",
    s"Incorrect appType for ${(name in `lagom-scala-impl`).value}: ${(appType in `lagom-scala-impl`).value}"
  )
}

TaskKey[Unit]("checkServiceDiscoveryLibraries") := {
  def isSame(left: ModuleID, right: ModuleID): Boolean =
    left.organization == right.organization &&
      left.name == right.name &&
      left.revision == right.revision

  val rpCommon: ModuleID = "com.lightbend.rp" %% "reactive-lib-common" % (reactiveLibVersion in `simple-app`).value
  val rpLagomServiceLocatorJava: ModuleID = "com.lightbend.rp" %% "reactive-lib-service-discovery-lagom14-java" % (reactiveLibVersion in `lagom-java-impl`).value
  val rpLagomServiceLocatorScala: ModuleID = "com.lightbend.rp" %% "reactive-lib-service-discovery-lagom14-scala" % (reactiveLibVersion in `lagom-scala-impl`).value
  val rpServiceLocator: ModuleID = "com.lightbend.rp" %% "reactive-lib-service-discovery" % (reactiveLibVersion in `frontend-play`).value

  val simpleAppLibs = (allDependencies in `simple-app`).value
  val frontendLibs = (allDependencies in frontend).value
  val frontendPlayLibs = (allDependencies in `frontend-play`).value
  val lagomJavaImplLibs = (allDependencies in `lagom-java-impl`).value
  val lagomScalaImplLibs = (allDependencies in `lagom-scala-impl`).value

  assert(
    simpleAppLibs.exists(isSame(_, rpCommon)),
    s"Unable to find [$rpCommon] in basic project: $simpleAppLibs"
  )

  assert(
    !simpleAppLibs.exists(isSame(_, rpServiceLocator)),
    s"Should not find [$rpServiceLocator] in basic project: $simpleAppLibs"
  )

  assert(
    !simpleAppLibs.exists(isSame(_, rpLagomServiceLocatorJava)),
    s"Should not find [$rpLagomServiceLocatorJava] in basic project: $simpleAppLibs"
  )

  assert(
    !simpleAppLibs.exists(isSame(_, rpLagomServiceLocatorScala)),
    s"Should not find [$rpLagomServiceLocatorScala] in basic project: $simpleAppLibs"
  )

  assert(
    frontendLibs.exists(isSame(_, rpLagomServiceLocatorScala)),
    s"Unable to find [$rpLagomServiceLocatorScala] in Lagom Play project: $frontendLibs"
  )

  assert(
    frontendPlayLibs.exists(isSame(_, rpServiceLocator)),
    s"Unable to find [$rpServiceLocator] in Play project: $frontendPlayLibs"
  )

  assert(
    !frontendLibs.exists(isSame(_, rpLagomServiceLocatorJava)),
    s"Should not find [$rpLagomServiceLocatorJava] in Play project: $frontendLibs"
  )

  assert(
    !frontendPlayLibs.exists(isSame(_, rpLagomServiceLocatorScala)),
    s"Should not find [$rpLagomServiceLocatorScala] in Play project: $frontendLibs"
  )

  assert(
    lagomJavaImplLibs.exists(isSame(_, rpLagomServiceLocatorJava)),
    s"Unable to find [$rpLagomServiceLocatorJava] in Lagom Java project: $lagomJavaImplLibs"
  )

  assert(
    !lagomJavaImplLibs.exists(isSame(_, rpLagomServiceLocatorScala)),
    s"Should not find [$rpLagomServiceLocatorScala] in Lagom Java project: $lagomJavaImplLibs"
  )

  assert(
    lagomScalaImplLibs.exists(isSame(_, rpLagomServiceLocatorScala)),
    s"Unable to find [$rpLagomServiceLocatorScala] in Lagom Scala project: $lagomScalaImplLibs"
  )

  assert(
    !lagomScalaImplLibs.exists(isSame(_, rpLagomServiceLocatorJava)),
    s"Should not find [$rpLagomServiceLocatorJava] in Lagom Scala project: $lagomScalaImplLibs"
  )

}

import sbt.IO
import ReleaseTransformations._
import scala.collection.immutable.Seq

val Versions = new {
  val crossSbtVersions = Vector("0.13.17", "1.1.6")
  val nativePackager   = "1.3.2"
  val playJson         = "2.6.5"
  val scala            = "2.12.4"
  val scalaTest        = "3.0.5"
}

ThisBuild / organization := "com.lightbend.rp"
ThisBuild / organizationName := "Lightbend, Inc."
ThisBuild / startYear := Some(2017)
ThisBuild / licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://www.lightbend.com/"))
ThisBuild / developers := List(
  Developer("lightbend", "Lightbend Contributors", "", url("https://github.com/lightbend/sbt-reactive-app"))
)
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/lightbend/sbt-reactive-app"), "git@github.com:lightbend/sbt-reactive-app.git"))

lazy val root = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin, SbtPlugin)
  .settings(nocomma {
    name := "sbt-reactive-app"

    scalaVersion in Global := Versions.scala
    crossSbtVersions := Versions.crossSbtVersions
    scalacOptions ++= Vector("-deprecation")

    libraryDependencies ++= Vector(
      "com.typesafe.play" %% "play-json" % Versions.playJson,
      "org.scalatest"     %% "scalatest" % Versions.scalaTest % "test"
    )

    sourceGenerators in Compile += Def.task {
      val versionFile = (sourceManaged in Compile).value / "ProgramVersion.scala"

      val versionSource =
        """|package com.lightbend.rp.sbtreactiveapp
          |
          |object ProgramVersion {
          |  val current = "%s"
          |}
        """.stripMargin.format(version.value)

      IO.write(versionFile, versionSource)

      Seq(versionFile)
      
    }

    addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % Versions.nativePackager)

    publishMavenStyle := true

    sonatypeProfileName := "com.lightbend.rp"
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    )
    releasePublishArtifactsAction := PgpKeys.publishSigned.value
    releaseCrossBuild := false
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("^publishSigned"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )

    // pass in -Ddeckhand.openshift to run scripted test with -Ddeckhand.openshift
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value) ++
      sys.props.get("deckhand.openshift").toList.map(_ => "-Ddeckhand.openshift")
    }
  })

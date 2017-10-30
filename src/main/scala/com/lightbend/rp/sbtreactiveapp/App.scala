/*
 * Copyright 2017 Lightbend, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightbend.rp.sbtreactiveapp

import sbt._
import scala.collection.immutable.Seq

import Keys._

sealed trait App extends SbtReactiveAppKeys {
  def projectSettings: Seq[Setting[_]] = Vector(
    nrOfCpus := None,
    diskSpace := None,
    memory := None,
    endpoints := Map.empty,
    volumes := Map.empty,
    privileged := false,
    healthCheck := None,
    readinessCheck := None,
    environmentVariables := Map.empty,
    startScriptLocation := Some("/rp-start"),
    secrets := Set.empty,
    reactiveLibProject := Some("basic"),
    reactiveLibVersion := Some("0.1.0-SNAPSHOT"),
    libraryDependencies ++= (
      for {
        project <- reactiveLibProject.value.toSeq
        version <- reactiveLibVersion.value.toSeq
      } yield "com.lightbend.rp" % project % version
    )
  )
}

sealed trait LagomApp extends App {
  val apiTools = config("api-tools").hide

  override def projectSettings: Seq[Setting[_]] = {
    // managedClasspath in "api-tools" contains the api tools library dependencies
    // fullClasspath contains the Lagom services, Lagom framework and all its dependencies

    super.projectSettings ++ Vector(
      managedClasspath in apiTools :=
        Classpaths.managedJars(apiTools, (classpathTypes in apiTools).value, update.value),

      endpoints := magic.Lagom.endpoints(
        ((managedClasspath in apiTools).value ++ (fullClasspath in Compile).value).toVector,
        scalaInstance.value.loader
      )
      .getOrElse(Map.empty)
    )
  }
}

case object LagomJavaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ Vector(
      reactiveLibProject := magic.Lagom.version.map(v => s"reactive-lib-lagom${SemVer.formatMajorMinor(v)}-java")
    )
}

case object LagomScalaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ Vector(
      reactiveLibProject := magic.Lagom.version.map(v => s"reactive-lib-lagom${SemVer.formatMajorMinor(v)}-scala")
    )
}

case object LagomPlayJavaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ Vector(
      reactiveLibProject := magic.Lagom.version.map(v => s"reactive-lib-lagom${SemVer.formatMajorMinor(v)}-java")
    )
}

case object LagomPlayScalaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ Vector(
      reactiveLibProject := magic.Lagom.version.map(v => s"reactive-lib-lagom${SemVer.formatMajorMinor(v)}-scala")
    )
}

case object PlayApp extends App {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ Vector(
      reactiveLibProject := magic.Play.version.map(v => s"reactive-lib-play${SemVer.formatMajorMinor(v)}")
    )
}

case object BasicApp extends App

object App {
  def apply: App =
    if (magic.Lagom.isPlayJava)
      LagomPlayJavaApp
    else if (magic.Lagom.isPlayScala)
      LagomPlayScalaApp
    else if (magic.Lagom.isJava)
      LagomJavaApp
    else if (magic.Lagom.isScala)
      LagomScalaApp
    else if (magic.Play.isPlay)
      PlayApp
    else
      BasicApp
}

private object SemVer {
  def formatMajorMinor(version: String): String = version.filterNot(_ == '.').take(2)

  def parse(version: String): Option[(Int, Int, Int, Option[String])] = {
    val parts = version.split("\\.", 3)

    if (
      parts.length == 3 &&
        parts(0).forall(_.isDigit) &&
        parts(1).forall(_.isDigit) &&
        parts(2).takeWhile(_ != '-').forall(_.isDigit)) {
      val major = parts(0).toInt
      val minor = parts(1).toInt
      val patchParts = parts(2).split("-", 2)

      val (patch, label) =
        if (patchParts.length == 2)
          (patchParts(0).toInt, Some(patchParts(1)))
        else
          (parts(2).toInt, None)

      Some((major, minor, patch, label))
    } else {
      None
    }
  }
}
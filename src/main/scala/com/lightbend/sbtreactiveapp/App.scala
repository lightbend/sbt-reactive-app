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

package com.lightbend.sbtreactiveapp

import sbt._
import scala.collection.immutable.Seq

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
    environmentVariables := Map.empty
  )
}

case object LagomScalaApp extends App

case object LagomJavaApp extends App

case object LagomPlayScalaApp extends App

case object LagomPlayJavaApp extends App

case object PlayApp extends App

case object BasicApp extends App

object App {
  def apply: App = {
    val mapping = Map(
      "com.lightbend.lagom.sbt.LagomPlayScala$" -> LagomPlayScalaApp,
      "com.lightbend.lagom.sbt.LagomPlayJava$" -> LagomPlayJavaApp,
      "com.lightbend.lagom.sbt.LagomScala$" -> LagomScalaApp,
      "com.lightbend.lagom.sbt.LagomJava$" -> LagomJavaApp,
      "play.sbt.Play$" -> PlayApp
    )

    val classLoader = App.getClass.getClassLoader

    mapping
      .find(pair => objectExists(classLoader, pair._1))
      .map(_._2)
      .getOrElse(BasicApp)
  }
}
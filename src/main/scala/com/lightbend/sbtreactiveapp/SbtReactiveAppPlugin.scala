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

import com.typesafe.sbt.packager.docker
import sbt._

object SbtReactiveAppPlugin extends AutoPlugin {
  object autoImport extends docker.DockerKeys with SbtReactiveAppKeys

  import autoImport._

  override def requires = docker.DockerPlugin

  override def trigger = allRequirements

  val Docker = docker.DockerPlugin.autoImport.Docker

  override def projectSettings: Seq[Setting[_]] =
    App.apply.projectSettings ++ Vector(
      dockerCommands := {
        dockerCommands.value ++ SbtReactiveApp
          .labels(
            diskSpace = diskSpace.value,
            memory = memory.value,
            nrOfCpus = nrOfCpus.value,
            endpoints = endpoints.value,
            volumes = volumes.value,
            privileged = privileged.value,
            healthCheck = healthCheck.value,
            readinessCheck = readinessCheck.value,
            environmentVariables = environmentVariables.value
          )
          .map { case (key, value) =>
            docker.Cmd("LABEL", s"""$key="${encodeLabelValue(value)}"""")
          }
      }
    )

  private def encodeLabelValue(value: String) =
    value
      .replaceAllLiterally("\n", "\\\n")
      .replaceAllLiterally("\"", "\\\"")
}

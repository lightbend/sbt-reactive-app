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

import com.typesafe.sbt.packager.docker
import sbt._

object SbtReactiveAppPlugin extends AutoPlugin {
  object autoImport extends SbtReactiveAppKeys {
    type Acl = com.lightbend.rp.sbtreactiveapp.Acl
    type HttpAcl = com.lightbend.rp.sbtreactiveapp.HttpAcl
    type TcpAcl = com.lightbend.rp.sbtreactiveapp.TcpAcl
    type UdpAcl = com.lightbend.rp.sbtreactiveapp.UdpAcl

    type Check = com.lightbend.rp.sbtreactiveapp.Check
    type CommandCheck = com.lightbend.rp.sbtreactiveapp.CommandCheck
    type HttpCheck = com.lightbend.rp.sbtreactiveapp.HttpCheck
    type TcpCheck = com.lightbend.rp.sbtreactiveapp.TcpCheck

    type Endpoint = com.lightbend.rp.sbtreactiveapp.Endpoint

    type EnvironmentVariable = com.lightbend.rp.sbtreactiveapp.EnvironmentVariable
    type LiteralEnvironmentVariable = com.lightbend.rp.sbtreactiveapp.LiteralEnvironmentVariable
    type SecretEnvironmentVariable = com.lightbend.rp.sbtreactiveapp.SecretEnvironmentVariable

    type Volume = com.lightbend.rp.sbtreactiveapp.Volume
    type HostPathVolume = com.lightbend.rp.sbtreactiveapp.HostPathVolume
    type SecretVolume = com.lightbend.rp.sbtreactiveapp.SecretVolume

    val HttpAcl = com.lightbend.rp.sbtreactiveapp.HttpAcl
    val TcpAcl = com.lightbend.rp.sbtreactiveapp.TcpAcl
    val UdpAcl = com.lightbend.rp.sbtreactiveapp.UdpAcl

    val CommandCheck = com.lightbend.rp.sbtreactiveapp.CommandCheck
    val HttpCheck = com.lightbend.rp.sbtreactiveapp.HttpCheck
    val TcpCheck = com.lightbend.rp.sbtreactiveapp.TcpCheck

    val Endpoint = com.lightbend.rp.sbtreactiveapp.Endpoint

    val LiteralEnvironmentVariable = com.lightbend.rp.sbtreactiveapp.LiteralEnvironmentVariable
    val SecretEnvironmentVariable = com.lightbend.rp.sbtreactiveapp.SecretEnvironmentVariable

    val HostPathVolume = com.lightbend.rp.sbtreactiveapp.HostPathVolume
    val SecretVolume = com.lightbend.rp.sbtreactiveapp.SecretVolume
  }

  object localImport extends docker.DockerKeys

  import autoImport._
  import localImport._

  override def requires = docker.DockerPlugin

  override def trigger = allRequirements

  val Docker = docker.DockerPlugin.autoImport.Docker

  override def projectSettings: Seq[Setting[_]] =
    App.apply.projectSettings ++ Vector(
      dockerCommands := {
        dockerCommands.value ++ SbtReactiveApp
          .labels(
            appName = Some(Keys.name.value),
            diskSpace = diskSpace.value,
            memory = memory.value,
            nrOfCpus = nrOfCpus.value,
            endpoints = endpoints.value,
            volumes = volumes.value,
            privileged = privileged.value,
            healthCheck = healthCheck.value,
            readinessCheck = readinessCheck.value,
            environmentVariables = environmentVariables.value,
            version = SemVer.parse(Keys.version.value)
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

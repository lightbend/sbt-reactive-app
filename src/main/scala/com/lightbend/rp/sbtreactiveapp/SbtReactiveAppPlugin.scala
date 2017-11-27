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

import com.typesafe.sbt.SbtNativePackager
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.scripts.AshScriptPlugin
import com.typesafe.sbt.packager.docker
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import scala.collection.{ Seq => DefaultSeq }
import scala.collection.immutable.Seq

object SbtReactiveAppPluginAll extends AutoPlugin {
  override def requires = JvmPlugin

  override def trigger = allRequirements

  override def projectSettings: DefaultSeq[Setting[_]] =
    inConfig(docker.DockerPlugin.autoImport.Docker)(
      publish in docker.DockerPlugin.autoImport.Docker := {
        Def.taskDyn {
          dockerRepository.?.value.nonEmpty && rpDockerPublish.?.value.nonEmpty

          Def.task(())
        }
      }.value)
}

object SbtReactiveAppPlugin extends AutoPlugin {
  object autoImport extends SbtReactiveAppKeys {
    type Ingress = com.lightbend.rp.sbtreactiveapp.Ingress

    type PortIngress = com.lightbend.rp.sbtreactiveapp.PortIngress
    val PortIngress = com.lightbend.rp.sbtreactiveapp.PortIngress

    type HttpIngress = com.lightbend.rp.sbtreactiveapp.HttpIngress
    val HttpIngress = com.lightbend.rp.sbtreactiveapp.HttpIngress

    type Check = com.lightbend.rp.sbtreactiveapp.Check

    type CommandCheck = com.lightbend.rp.sbtreactiveapp.CommandCheck
    val CommandCheck = com.lightbend.rp.sbtreactiveapp.CommandCheck

    type HttpCheck = com.lightbend.rp.sbtreactiveapp.HttpCheck
    val HttpCheck = com.lightbend.rp.sbtreactiveapp.HttpCheck

    type TcpCheck = com.lightbend.rp.sbtreactiveapp.TcpCheck
    val TcpCheck = com.lightbend.rp.sbtreactiveapp.TcpCheck

    type Endpoint = com.lightbend.rp.sbtreactiveapp.Endpoint

    type HttpEndpoint = com.lightbend.rp.sbtreactiveapp.HttpEndpoint
    val HttpEndpoint = com.lightbend.rp.sbtreactiveapp.HttpEndpoint

    type TcpEndpoint = com.lightbend.rp.sbtreactiveapp.TcpEndpoint
    val TcpEndpoint = com.lightbend.rp.sbtreactiveapp.TcpEndpoint

    type UdpEndpoint = com.lightbend.rp.sbtreactiveapp.UdpEndpoint
    val UdpEndpoint = com.lightbend.rp.sbtreactiveapp.UdpEndpoint

    type EnvironmentVariable = com.lightbend.rp.sbtreactiveapp.EnvironmentVariable

    type LiteralEnvironmentVariable = com.lightbend.rp.sbtreactiveapp.LiteralEnvironmentVariable
    val LiteralEnvironmentVariable = com.lightbend.rp.sbtreactiveapp.LiteralEnvironmentVariable

    type Volume = com.lightbend.rp.sbtreactiveapp.Volume

    type HostPathVolume = com.lightbend.rp.sbtreactiveapp.HostPathVolume
    val HostPathVolume = com.lightbend.rp.sbtreactiveapp.HostPathVolume

    type Secret = com.lightbend.rp.sbtreactiveapp.Secret
    val Secret = com.lightbend.rp.sbtreactiveapp.Secret

    object kubernetes {
      type ConfigMapEnvironmentVariable = com.lightbend.rp.sbtreactiveapp.kubernetes.ConfigMapEnvironmentVariable
      val ConfigMapEnvironmentVariable = com.lightbend.rp.sbtreactiveapp.kubernetes.ConfigMapEnvironmentVariable

      type FieldRefEnvironmentVariable = com.lightbend.rp.sbtreactiveapp.kubernetes.FieldRefEnvironmentVariable
      val FieldRefEnvironmentVariable = com.lightbend.rp.sbtreactiveapp.kubernetes.FieldRefEnvironmentVariable
    }
  }

  object localImport extends docker.DockerKeys

  import autoImport._
  import localImport._
  import docker.DockerPlugin._

  override def requires = SbtReactiveAppPluginAll && docker.DockerPlugin && AshScriptPlugin && SbtReactiveAppPluginAll

  override def trigger = noTrigger

  val Docker = docker.DockerPlugin.autoImport.Docker

  val localName = "rp-start"

  override def projectSettings: Seq[Setting[_]] =
    App.apply.projectSettings ++ Vector(
      resourceGenerators in Compile += Def.task {
        val file = (resourceManaged in Compile).value / "app.conf"

        IO.write(file, readResource("app.conf"))

        Seq(file)
      }.taskValue,

      javaOptions in SbtNativePackager.Universal ++= Vector(
        "-Dconfig.resource=app.conf"),

      dockerEntrypoint := startScriptLocation.value.fold(dockerEntrypoint.value)(_ +: dockerEntrypoint.value),

      dockerBaseImage := "openjdk:8-jre-alpine",

      dockerCommands := {
        val addCommand = startScriptLocation
          .value
          .toVector
          .map(path => docker.Cmd("COPY", localName, path))

        val bootstrapEnabled = enableAkkaClusterBootstrap.value.getOrElse(akkaClusterBootstrapEnabled.value)
        val commonEnabled = enableCommon.value
        val playHttpBindingEnabled = enablePlayHttpBinding.value
        val secretsEnabled = enableSecrets.value.getOrElse(secrets.value.nonEmpty)
        val serviceDiscoveryEnabled = enableServiceDiscovery.value

        dockerCommands.value ++ addCommand ++ SbtReactiveApp
          .labels(
            namespace = namespace.value,
            appName = Some(appName.value),
            appType = Some(appType.value),
            diskSpace = diskSpace.value,
            memory = memory.value,
            nrOfCpus = nrOfCpus.value,
            endpoints = endpoints.value,
            volumes = volumes.value,
            privileged = privileged.value,
            healthCheck = healthCheck.value,
            readinessCheck = readinessCheck.value,
            environmentVariables = environmentVariables.value,
            version = SemVer.parse(Keys.version.value),
            secrets = secrets.value,
            modules = Seq(
              "akka-cluster-bootstrapping" -> bootstrapEnabled,
              "common" -> commonEnabled,
              "play-http-binding" -> playHttpBindingEnabled,
              "secrets" -> secretsEnabled,
              "service-discovery" -> serviceDiscoveryEnabled))
          .map {
            case (key, value) =>
              docker.Cmd("LABEL", s"""$key="${encodeLabelValue(value)}"""")
          }
      }) ++ inConfig(Docker)(Seq(
        stage := {
          val target = stage.value
          val localPath = target / localName

          IO.write(localPath, readResource(localName))

          localPath.setExecutable(true)

          target
        },
        rpDockerPublish := {
          val _ = publishLocal.value
          val alias = dockerAlias.value
          val log = streams.value.log
          val execCommand = dockerExecCommand.value

          publishDocker(execCommand, alias.versioned, log)

          if (dockerUpdateLatest.value) {
            publishDocker(execCommand, alias.latest, log)
          }
        }))

  private def encodeLabelValue(value: String) =
    value
      .replaceAllLiterally("\n", "\\\n")
      .replaceAllLiterally("\"", "\\\"")

  private def readResource(name: String): String =
    scala.io.Source
      .fromInputStream(getClass.getClassLoader.getResourceAsStream(name))
      .mkString
}

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

import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.scripts.BashStartScriptPlugin
import com.typesafe.sbt.packager.docker
import sbt.{ Def, _ }
import sbt.Keys._
import sbt.plugins.JvmPlugin

import scala.collection.{ Seq => DefaultSeq }
import scala.collection.immutable.Seq
import scala.util.{ Failure, Success }

object SbtReactiveAppPluginAll extends AutoPlugin {
  override def requires = JvmPlugin

  override def trigger = allRequirements

  override def projectSettings: DefaultSeq[Setting[_]] =
    inConfig(docker.DockerPlugin.autoImport.Docker)(
      publish in docker.DockerPlugin.autoImport.Docker := {
        Def.taskDyn {
          if (dockerUsername.?.value.isDefined || dockerRepository.?.value.isDefined)
            Def.task(rpDockerPublish.value)
          else Def.task(())
        }.value
      })
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

  override def requires = SbtReactiveAppPluginAll && docker.DockerPlugin && BashStartScriptPlugin && SbtReactiveAppPluginAll

  override def trigger = noTrigger

  val Docker = docker.DockerPlugin.autoImport.Docker

  val localName = "rp-start"

  override def globalSettings: Seq[Setting[_]] = BasicApp.globalSettings

  override def buildSettings: Seq[Setting[_]] = BasicApp.buildSettings

  override def projectSettings: Seq[Setting[_]] = BasicApp.projectSettings
}

object SbtReactiveAppLagomScalaPlugin extends AutoPlugin {
  private val classLoader = this.getClass.getClassLoader

  override def requires = magic.Lagom.lagomScalaPlugin(classLoader) match {
    case Success(plugin) => SbtReactiveAppPlugin && plugin
    case Failure(_) => NoOpPlugin
  }

  override def trigger = allRequirements

  override def projectSettings = LagomScalaApp.projectSettings
}

object SbtReactiveAppLagomJavaPlugin extends AutoPlugin {
  private val classLoader = this.getClass.getClassLoader

  override def requires = magic.Lagom.lagomJavaPlugin(classLoader) match {
    case Success(plugin) => SbtReactiveAppPlugin && plugin
    case Failure(_) => NoOpPlugin
  }

  override def trigger = allRequirements

  override def projectSettings = LagomJavaApp.projectSettings
}

object SbtReactiveAppPlayLagomScalaPlugin extends AutoPlugin {
  private val classLoader = this.getClass.getClassLoader

  override def requires =
    magic
      .Lagom
      .lagomPlayScalaPlugin(classLoader)
      .flatMap(lagom => magic.Play.playPlugin(classLoader).map(play => Seq(lagom, play))) match {
        case Success(plugins) => plugins.foldLeft[Plugins](SbtReactiveAppPlugin)(_ && _)
        case Failure(_) => NoOpPlugin
      }

  override def trigger = allRequirements

  override def projectSettings = LagomPlayScalaApp.projectSettings
}

object SbtReactiveAppPlayLagomJavaPlugin extends AutoPlugin {
  private val classLoader = this.getClass.getClassLoader

  override def requires =
    magic
      .Lagom
      .lagomPlayJavaPlugin(classLoader)
      .flatMap(lagom => magic.Play.playPlugin(classLoader).map(play => Seq(lagom, play))) match {
        case Success(plugins) => plugins.foldLeft[Plugins](SbtReactiveAppPlugin)(_ && _)
        case Failure(_) => NoOpPlugin
      }

  override def trigger = allRequirements

  override def projectSettings = LagomPlayJavaApp.projectSettings
}

object SbtReactiveAppPlayPlugin extends AutoPlugin {
  private val classLoader = this.getClass.getClassLoader

  override def requires = magic.Play.playPlugin(classLoader) match {
    case Success(plugin) => SbtReactiveAppPlugin && plugin
    case Failure(_) => NoOpPlugin
  }

  override def trigger = allRequirements

  override def projectSettings = PlayApp.projectSettings
}

/**
 * This plugin is created so other plugins which depends on the [[NoOpPlugin]] will be disabled, as it's not possible
 * to enable [[NoOpPlugin]] due to its private access modifier.
 */
private[sbtreactiveapp] object NoOpPlugin extends AutoPlugin {
  override def trigger = noTrigger
}

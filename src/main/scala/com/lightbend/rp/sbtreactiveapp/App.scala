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

import com.lightbend.rp.sbtreactiveapp.SbtReactiveAppPlugin.localImport._
import com.lightbend.rp.sbtreactiveapp.SbtReactiveAppPlugin._
import com.typesafe.sbt.SbtNativePackager
import com.typesafe.sbt.packager.docker
import com.typesafe.sbt.packager.docker.DockerPlugin.{ publishDocker, publishLocalDocker }
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{ dockerAlias, dockerBuildCommand, dockerPermissionStrategy }
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport.defaultLinuxInstallLocation
import com.typesafe.sbt.packager.Keys.{ executableScriptName, daemonGroup, daemonUser, stage }
import sbt._

import scala.collection.immutable.Seq
import Keys._
import com.typesafe.sbt.packager.docker.DockerSupport

object App {
  private[sbtreactiveapp] val defaultReactiveLibVersion = "1.7.0"

  private val ValidNameChars =
    (('0' to '9') ++ ('A' to 'Z') ++ ('a' to 'z') ++ Seq('-')).toSet

  private val NameTrimChars = Set('-')

  private[sbtreactiveapp] def normalizeName(name: String): String =
    name
      .map(c => if (ValidNameChars.contains(c)) c else '-')
      .dropWhile(NameTrimChars.contains)
      .reverse
      .dropWhile(NameTrimChars.contains)
      .reverse
      .toLowerCase
}

trait App extends SbtReactiveAppKeys {
  private def libIsPublished(scalaVersion: String) =
    SemVer
      .parse(scalaVersion)
      .fold(false) { case (major, minor, _, _) => major >= 2 && minor >= 11 }

  private def lib(
    scalaVersion: String,
    nameAndCross: (String, Boolean),
    version: String,
    filter: Boolean): Seq[ModuleID] =
    if (filter && nameAndCross._2 && libIsPublished(scalaVersion))
      Seq("com.lightbend.rp" %% nameAndCross._1 % version)
    else if (filter && libIsPublished(scalaVersion))
      Seq("com.lightbend.rp" % nameAndCross._1 % version)
    else
      Seq.empty
}

sealed trait LagomApp extends App {
  val ApiTools = config("api-tools").hide

  def projectSettings: Seq[Setting[_]] =
    Vector(
      // For naming Lagom services, we take this overall approach:
      // Calculate the endpoints (rpLagomRawEndpoints) and make this the "appName"

      rpAppName := rpLagomRawEndpoints.value.headOption.map(_.name).getOrElse(name.value),

      rpAppType := "lagom",

      rpEnableAkkaClusterBootstrap := magic.Lagom.hasCluster(libraryDependencies.value.toVector),

      rpEnableServiceDiscovery := true,

      ivyConfigurations += ApiTools,

      managedClasspath in ApiTools :=
        Classpaths.managedJars(ApiTools, (classpathTypes in ApiTools).value, update.value),

      libraryDependencies ++= magic.Lagom.component("api-tools").toVector.map(_ % ApiTools),

      rpLagomRawEndpoints := {
        val ingressPorts = rpHttpIngressPorts.value
        val ingressHosts = rpHttpIngressHosts.value
        val ingressPaths = rpHttpIngressPaths.value
        val endpointName = name.value
        val config = rpApplicationConfig.value
        val servicePort = config.getInt("play.server.http.port")

        val magicEndpoints =
          magic.Lagom.endpoints(
            ((managedClasspath in ApiTools).value ++ (fullClasspath in Compile).value).toVector,
            scalaInstance.value.loader,
            servicePort,
            ingressPorts.toVector,
            ingressHosts.toVector,
            ingressPaths.toVector)
            .getOrElse(Seq.empty)

        // If we don't have any magic endpoints, we need to add one for the Play server

        if (magicEndpoints.nonEmpty)
          magicEndpoints
        else if (ingressPaths.nonEmpty)
          Vector(HttpEndpoint(endpointName, servicePort, HttpIngress(ingressPorts, ingressHosts, ingressPaths)))
        else
          Vector(HttpEndpoint(endpointName, servicePort))
      },

      // Note: Play & Lagom need their endpoints defined first (see play-http-binding)

      rpEndpoints := {
        // We don't have any guarantees on plugin order between Play <-> Lagom so we check in both places

        val current = rpEndpoints.value.filterNot(_.name == "http")

        val lagom =
          rpLagomRawEndpoints.value.zipWithIndex.map {
            case (e, 0) => e.withName("http")
            case (e, _) => e
          }

        lagom ++ current
      })
}

case object LagomJavaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings :+
      (rpReactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom14-java" -> true)
}

case object LagomScalaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings :+
      (rpReactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom14-scala" -> true)
}

case object LagomPlayJavaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings :+
      (rpReactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom14-java" -> true)
}

case object LagomPlayScalaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings :+
      (rpReactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom14-scala" -> true)
}

case object PlayApp extends App {
  def projectSettings: Seq[Setting[_]] =
    Vector(
      rpAppType := "play",

      rpEndpoints := {
        // We don't have any guarantees on plugin order between Play <-> Lagom so we check in both places
        val current = rpEndpoints.value
        val paths = rpHttpIngressPaths.value
        val ports = rpHttpIngressPorts.value
        val hosts = rpHttpIngressHosts.value
        val config = rpApplicationConfig.value
        val port = config.getInt("play.server.http.port")

        if (current.exists(_.name == "http")) {
          current
        } else {
          val endpoint =
            if (paths.nonEmpty)
              HttpEndpoint("http", port, HttpIngress(ports, hosts, paths))
            else
              HttpEndpoint("http", port)

          endpoint +: current
        }
      })
}

case object BasicApp extends DeployableApp {
  def globalSettings: Seq[Setting[_]] =
    Vector(
      rpAnnotations := Map.empty,
      rpAlpinePackages := Vector.empty,
      rpAppType := "basic",
      rpCpu := 0.0D,
      rpDiskSpace := 0L,
      rpMemory := 0L,
      rpEnableCGroupMemoryLimit := true,
      rpPrivileged := false,
      rpEnvironmentVariables := Map.empty,
      rpSecrets := Set.empty,
      rpReactiveLibVersion := App.defaultReactiveLibVersion,
      rpReactiveLibAkkaClusterBootstrapProject := "reactive-lib-akka-cluster-bootstrap" -> true,
      rpReactiveLibCommonProject := "reactive-lib-common" -> true,
      rpReactiveLibSecretsProject := "reactive-lib-secrets" -> true,
      rpReactiveLibServiceDiscoveryProject := "reactive-lib-service-discovery" -> true,
      rpReactiveLibStatusProject := "reactive-lib-status" -> true,
      // requiredAlpinePackages := Vector("bash"),
      rpPrependRpConf := "application.conf",
      rpAkkaClusterBootstrapEndpointName := "remoting",
      rpAkkaClusterBootstrapSystemName := "",
      rpAkkaManagementEndpointName := "management",
      rpHttpIngressHosts := Seq.empty,
      rpHttpIngressPaths := Seq.empty,
      rpHttpIngressPorts := Seq.empty)

  def buildSettings: Seq[Setting[_]] =
    Vector(
      rpDeployMinikubeReactiveSandboxCqlStatements := Seq.empty,
      rpHelm := {
        import complete.DefaultParsers._
        val args = spaceDelimited("<arg>").parsed
        cmd.helm.invoke(streams.value.log, args.toVector)
      },
      rpKubectl := {
        import complete.DefaultParsers._
        val args = spaceDelimited("<arg>").parsed
        cmd.kubectl.invoke(streams.value.log, args.toVector)
      },
      rpMinikube := {
        import complete.DefaultParsers._
        val args = spaceDelimited("<arg>").parsed
        cmd.minikube.invoke(streams.value.log, args.toVector)
      },
      aggregate in rpHelm := false,
      aggregate in rpKubectl := false,
      aggregate in rpMinikube := false)

  // This dynamic task collects all of the unmanaged resources in the projects
  // that are dependent of thisProject, as well as those of thisProject itself.
  val unmanagedTransitive = Def.taskDyn {
    val subProjects = sbt.Classpaths.interSort(
      thisProjectRef.value, Compile,
      settingsData.value, buildDependencies.value).map { _._1 }
    unmanagedResources.all(
      ScopeFilter(
        inProjects(subProjects: _*),
        inConfigurations(Compile)))
  }

  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ Vector(
      rpAppName := name.value,
      rpApplications := Vector("default" -> Vector(s"bin/${executableScriptName.value}")),
      rpEnableAkkaClusterBootstrap := false,
      rpEnableAkkaManagement := rpEnableAkkaClusterBootstrap.value || rpEnableStatus.value,
      rpEnableCommon := true,
      rpEnableSecrets := rpSecrets.value.nonEmpty,
      rpEnableServiceDiscovery := rpEnableAkkaClusterBootstrap.value,
      rpEnableStatus := rpEnableAkkaClusterBootstrap.value,
      resourceGenerators in Compile += Def.task {
        val outFile = (resourceManaged in Compile).value / "sbt-reactive-app" / LocalApplicationConfig

        val cacheDir = streams.value.cacheDirectory
        val tempFile = cacheDir / "sbt-reactive-app" / LocalApplicationConfig

        val cachedCopyFile =
          Tracked.inputChanged(cacheDir / "sbt-reactive-app-inputs") { (inChanged, _: HashFileInfo) =>
            if (inChanged || !outFile.exists) {
              IO.copyFile(tempFile, outFile, preserveLastModified = true)
            }
          }

        val allApplicationConfFiles = unmanagedTransitive.value.flatten.toList
        val unmanagedConfigName = rpPrependRpConf.value
        val cp = (dependencyClasspath in Compile).value
        if (unmanagedConfigName.isEmpty) Nil
        else {
          // 1. make the file under cache/sbt-reactive-app.
          // 2. compare its SHA1 against cache/sbt-reactive-app-inputs
          IO.write(tempFile, magic.Build.extractRpToolingConf(
            Vector(ToolingConfig),
            cp,
            allApplicationConfFiles.nonEmpty,
            unmanagedConfigName))
          cachedCopyFile(FileInfo.hash(tempFile))
          Seq(outFile)
        }
      }.taskValue,

      mappings in (Compile, packageBin) +=
        (resourceManaged in Compile).value / "sbt-reactive-app" / LocalApplicationConfig -> LocalApplicationConfig,

      allDependencies :=
        allDependencies.value ++
        lib(scalaVersion.value, rpReactiveLibAkkaClusterBootstrapProject.value, rpReactiveLibVersion.value, rpEnableAkkaClusterBootstrap.value) ++
        lib(scalaVersion.value, rpReactiveLibCommonProject.value, rpReactiveLibVersion.value, rpEnableCommon.value) ++
        lib(scalaVersion.value, rpReactiveLibSecretsProject.value, rpReactiveLibVersion.value, rpEnableSecrets.value) ++
        lib(scalaVersion.value, rpReactiveLibServiceDiscoveryProject.value, rpReactiveLibVersion.value, rpEnableServiceDiscovery.value) ++
        lib(scalaVersion.value, rpReactiveLibStatusProject.value, rpReactiveLibVersion.value, rpEnableStatus.value),

      rpApplicationConfig := {
        val cp = (fullClasspath in Compile).value.toList.map(_.data)
        val allApplicationConfFiles = unmanagedTransitive.value.flatten.toList
        magic.Build.makeConfig(allApplicationConfFiles ++ cp)
      },

      rpEndpoints := {
        val remotingEndpointName = rpAkkaClusterBootstrapEndpointName.value
        val managementEndpointName = rpAkkaManagementEndpointName.value
        val bootstrapEnabled = rpEnableAkkaClusterBootstrap.value
        val managementEnabled = rpEnableAkkaManagement.value
        val config = rpApplicationConfig.value

        rpEndpoints.?.value.getOrElse(Seq.empty) ++
          (if (bootstrapEnabled) Seq(TcpEndpoint(remotingEndpointName, config.getInt("akka.remote.netty.tcp.port")))
          else Seq.empty) ++
          (if (managementEnabled) Seq(TcpEndpoint(managementEndpointName, config.getInt("akka.management.http.port")))
          else Seq.empty)
      },

      javaOptions in SbtNativePackager.Universal ++= (
        if (rpMemory.value > 0L && rpEnableCGroupMemoryLimit.value)
          Vector("-J-XX:+UnlockExperimentalVMOptions", "-J-XX:+UseCGroupMemoryLimitForHeap")
        else
          Vector.empty),

      dockerBaseImage := "openjdk:8-jre-alpine",

      rpStartScriptLocation := {
        val dockerBaseDirectory = (defaultLinuxInstallLocation in Docker).value
        dockerBaseDirectory + "/bin/" + localName
      },

      dockerEntrypoint := (
        if (rpStartScriptLocation.value.isEmpty)
          dockerEntrypoint.value
        else
          rpStartScriptLocation.value +: dockerEntrypoint.value),

      rpPackagingDockerCommmands := {
        val alpinePackagesValue = alpinePackages.value
        val requiredAlpinePackagesValue = Vector("bash", "shadow")
        val allAlpinePackages = (alpinePackagesValue ++ requiredAlpinePackagesValue).distinct.sorted
        if (allAlpinePackages.isEmpty)
          Vector.empty
        else
          Vector(docker.Cmd("RUN", Vector("/sbin/apk", "add", "--no-cache") ++ allAlpinePackages: _*))
      },

      rpPermissionsDockerCommmands := {
        dockerEntrypoint.value.toVector
          .take(2)
          .map(x => docker.ExecCmd("RUN", "chmod", "u+x,g+x", x))
      },

      dockerCommands := {
        val bootstrapEnabled = rpEnableAkkaClusterBootstrap.value
        val bootstrapSystemName = Some(rpAkkaClusterBootstrapSystemName.value).filter(_.nonEmpty && bootstrapEnabled)
        val commonEnabled = rpEnableCommon.value
        val secretsEnabled = rpEnableSecrets.value
        val serviceDiscoveryEnabled = rpEnableServiceDiscovery.value
        val statusEnabled = rpEnableStatus.value
        val akkaManagementEnabled = bootstrapEnabled || statusEnabled
        val rawDockerCommands = dockerCommands.value.toList
        val dockerVersionValue = dockerVersion.value
        val startScriptLocationValue = rpStartScriptLocation.value
        val addPackageCommands = rpPackagingDockerCommmands.value
        val addPermissionsCommands = rpPermissionsDockerCommmands.value
        val remotingEndpointName = rpAkkaClusterBootstrapEndpointName.value
        val managementEndpointName = rpAkkaManagementEndpointName.value
        val strategy = dockerPermissionStrategy.value

        val rawAndPackageCommands =
          if (rawDockerCommands.isEmpty) addPackageCommands
          else {
            // Inject addPackageCommands after the second FROM
            val lastIdx = lastIndex("FROM", rawDockerCommands)
            val commands1 =
              if (lastIdx == -1) addPackageCommands ++ rawDockerCommands
              else {
                val (xs, ys) = rawDockerCommands.splitAt(lastIdx + 1)
                xs ++ addPackageCommands ++ ys
              }
            val idx = if (strategy == docker.DockerPermissionStrategy.MultiStage) firstIndex("USER", commands1.toList)
            else firstIndex("COPY", commands1.toList)
            if (idx == -1) commands1
            else {
              val (xs, ys) = commands1.splitAt(idx + 1)
              xs ++ addPermissionsCommands ++ ys
            }
          }

        rawAndPackageCommands ++ labelCommand(SbtReactiveApp
          .labels(
            appName = Some(rpAppName.value),
            appType = Some(rpAppType.value),
            applications = rpApplications.value.toVector.map {
              case (aName, appValue) =>
                val script =
                  startScriptLocationValue

                val args =
                  (if (script.isEmpty) appValue else script +: appValue).toVector

                aName -> args
            },
            configResource = Some((rpPrependRpConf in Compile).value)
              .filter(_.nonEmpty)
              .map(_ => LocalApplicationConfig),
            diskSpace = if (rpDiskSpace.value > 0L) Some(rpDiskSpace.value) else None,
            memory = if (rpMemory.value > 0) Some(rpMemory.value) else None,
            cpu = if (rpCpu.value >= 0.0001D) Some(rpCpu.value) else None,
            endpoints = rpEndpoints.value.toVector,
            remotingEndpointName = if (bootstrapEnabled) Some(remotingEndpointName) else None,
            managementEndpointName = if (akkaManagementEnabled) Some(managementEndpointName) else None,
            privileged = rpPrivileged.value,
            environmentVariables = rpEnvironmentVariables.value,
            version = Some(Keys.version.value),
            secrets = rpSecrets.value,
            annotations = rpAnnotations.value,
            modules = Seq(
              "akka-cluster-bootstrapping" -> bootstrapEnabled,
              "akka-management" -> akkaManagementEnabled,
              "common" -> commonEnabled,
              "secrets" -> secretsEnabled,
              "service-discovery" -> serviceDiscoveryEnabled,
              "status" -> statusEnabled),
            akkaClusterBootstrapSystemName = bootstrapSystemName))
      }) ++ inConfig(Docker)(Seq(
        mappings += {
          val localPath = target.value / localName
          IO.write(localPath, readResource(localName))
          localPath.setExecutable(true)
          (localPath, rpStartScriptLocation.value)
        },
        rpDockerPublish := {
          val _ = publishLocal.value
          val alias = dockerAlias.value
          val log = streams.value.log
          val execCommand = dockerExecCommand.value

          publishDocker(execCommand, NativePackagerCompat.versioned(alias), log)

          if (dockerUpdateLatest.value) {
            publishDocker(execCommand, NativePackagerCompat.latest(alias), log)
          }
        }))

  private[sbtreactiveapp] def labelCommand(labels: Seq[(String, String)]) = {
    val entry =
      labels
        .map {
          case (l, v) =>
            s"""$l="${encodeLabelValue(v)}""""
        }
        .mkString(" \\\n")

    if (entry.isEmpty)
      Seq.empty
    else
      Seq(docker.Cmd("LABEL", entry))
  }

  private def libIsPublished(scalaVersion: String) =
    SemVer
      .parse(scalaVersion)
      .fold(false) { case (major, minor, _, _) => major >= 2 && minor >= 11 }

  private def lib(
    scalaVersion: String,
    nameAndCross: (String, Boolean),
    version: String,
    filter: Boolean): Seq[ModuleID] =
    if (filter && nameAndCross._2 && libIsPublished(scalaVersion))
      Seq("com.lightbend.rp" %% nameAndCross._1 % version)
    else if (filter && libIsPublished(scalaVersion))
      Seq("com.lightbend.rp" % nameAndCross._1 % version)
    else
      Seq.empty

  private def lastIndex(command: String, commands: List[docker.CmdLike]): Int = {
    val indices: List[Int] = (commands.zipWithIndex collect {
      case (docker.Cmd(c, _), idx) if c == command => idx
    })
    if (indices.isEmpty) -1
    else indices.max
  }

  private def firstIndex(command: String, commands: List[docker.CmdLike]): Int =
    commands indexWhere ({
      case docker.Cmd(c, _) => c == command
      case _ => false
    }: docker.CmdLike => Boolean)

  private def encodeLabelValue(value: String) =
    value
      .replaceAllLiterally("\n", "\\\n")
      .replaceAllLiterally("\"", "\\\"")

  private def readResource(name: String): String =
    scala.io.Source
      .fromInputStream(getClass.getClassLoader.getResourceAsStream(name))
      .mkString
}

private object SemVer {
  def parse(version: String): Option[(Int, Int, Int, Option[String])] = {
    val parts = version.split("\\.", 3)

    if (parts.length == 3 &&
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

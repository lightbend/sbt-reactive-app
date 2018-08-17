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
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{ dockerAlias, dockerBuildCommand }
import com.typesafe.sbt.packager.Keys.{ executableScriptName, daemonGroup, daemonUser, stage }
import sbt._

import scala.collection.immutable.Seq
import Keys._
import com.typesafe.sbt.packager.docker.DockerSupport

object App {
  private[sbtreactiveapp] val defaultReactiveLibVersion = "0.9.0"

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

      rpEnablePlayHttpBinding := true,

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

        val magicEndpoints =
          magic.Lagom.endpoints(
            ((managedClasspath in ApiTools).value ++ (fullClasspath in Compile).value).toVector,
            scalaInstance.value.loader,
            ingressPorts.toVector,
            ingressHosts.toVector,
            ingressPaths.toVector)
            .getOrElse(Seq.empty)

        // If we don't have any magic endpoints, we need to add one for the Play server

        if (magicEndpoints.nonEmpty)
          magicEndpoints
        else if (ingressPaths.nonEmpty)
          Vector(HttpEndpoint(endpointName, HttpIngress(ingressPorts, ingressHosts, ingressPaths)))
        else
          Vector(HttpEndpoint(endpointName))
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

      // Note: Play & Lagom need their endpoints defined first (see play-http-binding)

      rpEnablePlayHttpBinding := true,

      rpEndpoints := {
        // We don't have any guarantees on plugin order between Play <-> Lagom so we check in both places

        val current = rpEndpoints.value
        val paths = rpHttpIngressPaths.value
        val ports = rpHttpIngressPorts.value
        val hosts = rpHttpIngressHosts.value

        if (current.exists(_.name == "http")) {
          current
        } else {
          val endpoint =
            if (paths.nonEmpty)
              HttpEndpoint("http", HttpIngress(ports, hosts, paths))
            else
              HttpEndpoint("http")

          endpoint +: current
        }
      })
}

case object BasicApp extends DeployableApp {
  def globalSettings: Seq[Setting[_]] = Seq(
    rpAnnotations := Map.empty)

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

  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ Vector(
      rpAlpinePackages := Vector.empty,
      rpAppName := name.value,
      rpAppType := "basic",
      rpApplications := Vector("default" -> Vector(s"bin/${executableScriptName.value}")),
      rpCpu := 0.0D,
      rpDiskSpace := 0L,
      rpMemory := 0L,
      rpEnableCGroupMemoryLimit := true,
      rpPrivileged := false,
      rpRunAsUser := "daemon",
      rpRunAsUserGroup := "",
      rpRunAsUserUID := -1,
      rpRunAsUserGID := -1,
      rpEnvironmentVariables := Map.empty,
      rpStartScriptLocation := "/rp-start",
      rpSecrets := Set.empty,
      rpReactiveLibVersion := App.defaultReactiveLibVersion,
      rpReactiveLibAkkaClusterBootstrapProject := "reactive-lib-akka-cluster-bootstrap" -> true,
      rpReactiveLibCommonProject := "reactive-lib-common" -> true,
      rpReactiveLibPlayHttpBindingProject := "reactive-lib-play-http-binding" -> true,
      rpReactiveLibSecretsProject := "reactive-lib-secrets" -> true,
      rpReactiveLibServiceDiscoveryProject := "reactive-lib-service-discovery" -> true,
      rpReactiveLibStatusProject := "reactive-lib-status" -> true,
      rpRequiredAlpinePackages := Vector("bash"),
      rpEnableAkkaClusterBootstrap := false,
      rpEnableAkkaManagement := rpEnableAkkaClusterBootstrap.value || rpEnableStatus.value,
      rpEnableCommon := true,
      rpEnablePlayHttpBinding := false,
      rpEnableSecrets := rpSecrets.value.nonEmpty,
      rpEnableServiceDiscovery := rpEnableAkkaClusterBootstrap.value,
      rpEnableStatus := rpEnableAkkaClusterBootstrap.value,

      rpPrependRpConf := "application.conf",

      rpAkkaClusterBootstrapEndpointName := "akka-remote",

      rpAkkaClusterBootstrapSystemName := "",

      rpAkkaManagementEndpointName := "akka-mgmt-http",

      rpHttpIngressHosts := Seq.empty,

      rpHttpIngressPaths := Seq.empty,

      rpHttpIngressPorts := Seq(80, 443),

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

        val unmanagedConfigName = rpPrependRpConf.value
        if (unmanagedConfigName.isEmpty) Nil
        else {
          // 1. make the file under cache/sbt-reactive-app.
          // 2. compare its SHA1 against cache/sbt-reactive-app-inputs
          IO.write(tempFile, magic.Build.extractApplicationConf(
            Vector(ToolingConfig), Vector(unmanagedConfigName),
            (unmanagedResources in Compile).value, (dependencyClasspath in Compile).value)
            .getOrElse(""))
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
        lib(scalaVersion.value, rpReactiveLibPlayHttpBindingProject.value, rpReactiveLibVersion.value, rpEnablePlayHttpBinding.value) ++
        lib(scalaVersion.value, rpReactiveLibSecretsProject.value, rpReactiveLibVersion.value, rpEnableSecrets.value) ++
        lib(scalaVersion.value, rpReactiveLibServiceDiscoveryProject.value, rpReactiveLibVersion.value, rpEnableServiceDiscovery.value) ++
        lib(scalaVersion.value, rpReactiveLibStatusProject.value, rpReactiveLibVersion.value, rpEnableStatus.value),

      rpEndpoints := {
        val clusterEndpointName = rpAkkaClusterBootstrapEndpointName.value
        val managementEndpointName = rpAkkaManagementEndpointName.value
        val bootstrapEnabled = rpEnableAkkaClusterBootstrap.value
        val managementEnabled = rpEnableAkkaManagement.value

        rpEndpoints.?.value.getOrElse(Seq.empty) ++
          (if (bootstrapEnabled) Seq(TcpEndpoint(clusterEndpointName)) else Seq.empty) ++
          (if (managementEnabled) Seq(TcpEndpoint(managementEndpointName)) else Seq.empty)
      },

      javaOptions in SbtNativePackager.Universal ++= (
        if (rpMemory.value > 0L && rpEnableCGroupMemoryLimit.value)
          Vector("-J-XX:+UnlockExperimentalVMOptions", "-J-XX:+UseCGroupMemoryLimitForHeap")
        else
          Vector.empty),

      dockerEntrypoint := (
        if (rpStartScriptLocation.value.isEmpty)
          dockerEntrypoint.value
        else
          rpStartScriptLocation.value +: dockerEntrypoint.value),

      dockerBaseImage := "openjdk:8-jre-alpine",

      dockerEntrypoint := Vector.empty,

      (daemonUser in Docker) := rpRunAsUser.value,
      (daemonGroup in Docker) := (if (rpRunAsUserGroup.value.isEmpty) rpRunAsUser.value else rpRunAsUserGroup.value),

      dockerCommands := {
        val bootstrapEnabled = rpEnableAkkaClusterBootstrap.value
        val bootstrapSystemName = Some(rpAkkaClusterBootstrapSystemName.value).filter(_.nonEmpty && bootstrapEnabled)
        val commonEnabled = rpEnableCommon.value
        val playHttpBindingEnabled = rpEnablePlayHttpBinding.value
        val secretsEnabled = rpEnableSecrets.value
        val serviceDiscoveryEnabled = rpEnableServiceDiscovery.value
        val statusEnabled = rpEnableStatus.value
        val akkaManagementEnabled = bootstrapEnabled || statusEnabled
        val rawDockerCommands = dockerCommands.value
        val alpinePackagesValue = rpAlpinePackages.value
        val requiredAlpinePackagesValue = rpRequiredAlpinePackages.value
        val allAlpinePackages = (alpinePackagesValue ++ requiredAlpinePackagesValue).distinct.sorted
        val dockerVersionValue = dockerVersion.value
        val startScriptLocationValue = rpStartScriptLocation.value
        val group = (daemonGroup in Docker).value
        val user = (daemonUser in Docker).value

        val addPackageCommands =
          if (allAlpinePackages.isEmpty)
            Vector.empty
          else
            Vector(docker.Cmd("RUN", Vector("/sbin/apk", "add", "--no-cache") ++ allAlpinePackages: _*))

        val uidFlag = if (rpRunAsUserUID.value >= 0) s"-u ${rpRunAsUserUID.value} " else ""
        val gidFlag = if (rpRunAsUserGID.value >= 0) s"-g ${rpRunAsUserGID.value} " else ""
        val addUserCommands = Vector(
          docker.Cmd("RUN", s"id -g $group || addgroup ${gidFlag}$group"),
          docker.Cmd("RUN", s"id -u $user || adduser ${uidFlag}$user $group"))

        val copyCommands =
          if (startScriptLocationValue.isEmpty)
            Vector.empty
          else if (dockerVersionValue.exists(DockerSupport.chownFlag))
            Vector(docker.Cmd("COPY", s"--chown=$user:$group", localName, startScriptLocationValue))
          else
            Vector(
              docker.Cmd("COPY", localName, startScriptLocationValue),
              docker.ExecCmd("RUN", Vector("chown", "-R", s"$user:$group", startScriptLocationValue): _*))

        /**
         * Must create the user and add any packages before the rest of the Dockerfile.
         * Must contain COPY+chown commands before the rest of the Dockerfile (see [[DockerSupport.chownFlag]]).
         */
        val rawAndPackageAndUserCommands =
          if (rawDockerCommands.isEmpty)
            addPackageCommands ++ addUserCommands ++ copyCommands
          else
            // First line is "FROM" line, so we must place commands after it.
            rawDockerCommands.head +: (addPackageCommands ++ addUserCommands ++ copyCommands ++ rawDockerCommands.tail)

        rawAndPackageAndUserCommands ++ labelCommand(SbtReactiveApp
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
            privileged = rpPrivileged.value,
            environmentVariables = rpEnvironmentVariables.value,
            version = Some(Keys.version.value),
            secrets = rpSecrets.value,
            annotations = rpAnnotations.value,
            modules = Seq(
              "akka-cluster-bootstrapping" -> bootstrapEnabled,
              "akka-management" -> akkaManagementEnabled,
              "common" -> commonEnabled,
              "play-http-binding" -> playHttpBindingEnabled,
              "secrets" -> secretsEnabled,
              "service-discovery" -> serviceDiscoveryEnabled,
              "status" -> statusEnabled),
            akkaClusterBootstrapSystemName = bootstrapSystemName))
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

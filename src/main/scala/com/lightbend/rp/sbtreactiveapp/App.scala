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
  private[sbtreactiveapp] val defaultReactiveLibVersion = "1.7.0-M1"

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
      // Calculate the endpoints (lagomRawEndpoints) and make this the "appName"

      appName := lagomRawEndpoints.value.headOption.map(_.name).getOrElse(name.value),

      appType := "lagom",

      enableAkkaClusterBootstrap := magic.Lagom.hasCluster(libraryDependencies.value.toVector),

      enablePlayHttpBinding := true,

      enableServiceDiscovery := true,

      ivyConfigurations += ApiTools,

      managedClasspath in ApiTools :=
        Classpaths.managedJars(ApiTools, (classpathTypes in ApiTools).value, update.value),

      libraryDependencies ++= magic.Lagom.component("api-tools").toVector.map(_ % ApiTools),

      lagomRawEndpoints := {
        val ingressPorts = httpIngressPorts.value
        val ingressHosts = httpIngressHosts.value
        val ingressPaths = httpIngressPaths.value
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

      endpoints := {
        // We don't have any guarantees on plugin order between Play <-> Lagom so we check in both places

        val current = endpoints.value.filterNot(_.name == "http")

        val lagom =
          lagomRawEndpoints.value.zipWithIndex.map {
            case (e, 0) => e.withName("http")
            case (e, _) => e
          }

        lagom ++ current
      })
}

case object LagomJavaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings :+
      (reactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom14-java" -> true)
}

case object LagomScalaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings :+
      (reactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom14-scala" -> true)
}

case object LagomPlayJavaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings :+
      (reactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom14-java" -> true)
}

case object LagomPlayScalaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings :+
      (reactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom14-scala" -> true)
}

case object PlayApp extends App {
  def projectSettings: Seq[Setting[_]] =
    Vector(
      appType := "play",

      // Note: Play & Lagom need their endpoints defined first (see play-http-binding)

      enablePlayHttpBinding := true,

      endpoints := {
        // We don't have any guarantees on plugin order between Play <-> Lagom so we check in both places

        val current = endpoints.value
        val paths = httpIngressPaths.value
        val ports = httpIngressPorts.value
        val hosts = httpIngressHosts.value
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
      annotations := Map.empty,
      alpinePackages := Vector.empty,
      appType := "basic",
      cpu := 0.0D,
      diskSpace := 0L,
      memory := 0L,
      enableCGroupMemoryLimit := true,
      privileged := false,
      environmentVariables := Map.empty,
      secrets := Set.empty,
      reactiveLibVersion := App.defaultReactiveLibVersion,
      reactiveLibAkkaClusterBootstrapProject := "reactive-lib-akka-cluster-bootstrap" -> true,
      reactiveLibCommonProject := "reactive-lib-common" -> true,
      reactiveLibSecretsProject := "reactive-lib-secrets" -> true,
      reactiveLibServiceDiscoveryProject := "reactive-lib-service-discovery" -> true,
      reactiveLibStatusProject := "reactive-lib-status" -> true,
      // requiredAlpinePackages := Vector("bash"),
      prependRpConf := "application.conf",
      akkaClusterBootstrapEndpointName := "remoting",
      akkaClusterBootstrapSystemName := "",
      akkaManagementEndpointName := "management",
      httpIngressHosts := Seq.empty,
      httpIngressPaths := Seq.empty,
      httpIngressPorts := Seq(80, 443))

  def buildSettings: Seq[Setting[_]] =
    Vector(
      deployMinikubeReactiveSandboxCqlStatements := Seq.empty,
      helm := {
        import complete.DefaultParsers._
        val args = spaceDelimited("<arg>").parsed
        cmd.helm.invoke(streams.value.log, args.toVector)
      },
      kubectl := {
        import complete.DefaultParsers._
        val args = spaceDelimited("<arg>").parsed
        cmd.kubectl.invoke(streams.value.log, args.toVector)
      },
      minikube := {
        import complete.DefaultParsers._
        val args = spaceDelimited("<arg>").parsed
        cmd.minikube.invoke(streams.value.log, args.toVector)
      },
      aggregate in helm := false,
      aggregate in kubectl := false,
      aggregate in minikube := false)

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
      appName := name.value,
      applications := Vector("default" -> Vector(s"bin/${executableScriptName.value}")),
      enableAkkaClusterBootstrap := false,
      enableAkkaManagement := enableAkkaClusterBootstrap.value || enableStatus.value,
      enableCommon := true,
      enablePlayHttpBinding := false,
      enableSecrets := secrets.value.nonEmpty,
      enableServiceDiscovery := enableAkkaClusterBootstrap.value,
      enableStatus := enableAkkaClusterBootstrap.value,

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

        val unmanagedConfigName = prependRpConf.value
        if (unmanagedConfigName.isEmpty) Nil
        else {
          // 1. make the file under cache/sbt-reactive-app.
          // 2. compare its SHA1 against cache/sbt-reactive-app-inputs
          IO.write(tempFile, magic.Build.extractRpToolingConf(
            Vector(ToolingConfig),
            (dependencyClasspath in Compile).value,
            allApplicationConfFiles.nonEmpty,
            unmanagedConfigName).getOrElse(""))
          cachedCopyFile(FileInfo.hash(tempFile))
          Seq(outFile)
        }
      }.taskValue,

      mappings in (Compile, packageBin) +=
        (resourceManaged in Compile).value / "sbt-reactive-app" / LocalApplicationConfig -> LocalApplicationConfig,

      allDependencies :=
        allDependencies.value ++
        lib(scalaVersion.value, reactiveLibAkkaClusterBootstrapProject.value, reactiveLibVersion.value, enableAkkaClusterBootstrap.value) ++
        lib(scalaVersion.value, reactiveLibCommonProject.value, reactiveLibVersion.value, enableCommon.value) ++
        lib(scalaVersion.value, reactiveLibSecretsProject.value, reactiveLibVersion.value, enableSecrets.value) ++
        lib(scalaVersion.value, reactiveLibServiceDiscoveryProject.value, reactiveLibVersion.value, enableServiceDiscovery.value) ++
        lib(scalaVersion.value, reactiveLibStatusProject.value, reactiveLibVersion.value, enableStatus.value),

      rpApplicationConfig := {
        val cp = (fullClasspath in Compile).value.toList.map(_.data)
        val allApplicationConfFiles = unmanagedTransitive.value.flatten.toList
        magic.Build.makeConfig(allApplicationConfFiles ++ cp)
      },

      endpoints := {
        val remotingEndpointName = akkaClusterBootstrapEndpointName.value
        val managementEndpointName = akkaManagementEndpointName.value
        val bootstrapEnabled = enableAkkaClusterBootstrap.value
        val managementEnabled = enableAkkaManagement.value
        val config = rpApplicationConfig.value

        endpoints.?.value.getOrElse(Seq.empty) ++
          (if (bootstrapEnabled) Seq(TcpEndpoint(remotingEndpointName, config.getInt("akka.remote.netty.tcp.port")))
          else Seq.empty) ++
          (if (managementEnabled) Seq(TcpEndpoint(managementEndpointName, config.getInt("akka.management.http.port")))
          else Seq.empty)
      },

      javaOptions in SbtNativePackager.Universal ++= (
        if (memory.value > 0L && enableCGroupMemoryLimit.value)
          Vector("-J-XX:+UnlockExperimentalVMOptions", "-J-XX:+UseCGroupMemoryLimitForHeap")
        else
          Vector.empty),

      dockerBaseImage := "openjdk:8-jre-alpine",

      startScriptLocation := {
        val dockerBaseDirectory = (defaultLinuxInstallLocation in Docker).value
        dockerBaseDirectory + "/bin/" + localName
      },

      dockerEntrypoint := (
        if (startScriptLocation.value.isEmpty)
          dockerEntrypoint.value
        else
          startScriptLocation.value +: dockerEntrypoint.value),

      rpPackagingDockerCommmands := {
        val alpinePackagesValue = alpinePackages.value
        val requiredAlpinePackagesValue = Vector("bash")
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
        val bootstrapEnabled = enableAkkaClusterBootstrap.value
        val bootstrapSystemName = Some(akkaClusterBootstrapSystemName.value).filter(_.nonEmpty && bootstrapEnabled)
        val commonEnabled = enableCommon.value
        val playHttpBindingEnabled = enablePlayHttpBinding.value
        val secretsEnabled = enableSecrets.value
        val serviceDiscoveryEnabled = enableServiceDiscovery.value
        val statusEnabled = enableStatus.value
        val akkaManagementEnabled = bootstrapEnabled || statusEnabled
        val rawDockerCommands = dockerCommands.value.toList
        val dockerVersionValue = dockerVersion.value
        val startScriptLocationValue = startScriptLocation.value
        val addPackageCommands = rpPackagingDockerCommmands.value
        val addPermissionsCommands = rpPermissionsDockerCommmands.value
        val remotingEndpointName = akkaClusterBootstrapEndpointName.value
        val managementEndpointName = akkaManagementEndpointName.value
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
            appName = Some(appName.value),
            appType = Some(appType.value),
            applications = applications.value.toVector.map {
              case (aName, appValue) =>
                val script =
                  startScriptLocationValue

                val args =
                  (if (script.isEmpty) appValue else script +: appValue).toVector

                aName -> args
            },
            configResource = Some((prependRpConf in Compile).value)
              .filter(_.nonEmpty)
              .map(_ => LocalApplicationConfig),
            diskSpace = if (diskSpace.value > 0L) Some(diskSpace.value) else None,
            memory = if (memory.value > 0) Some(memory.value) else None,
            cpu = if (cpu.value >= 0.0001D) Some(cpu.value) else None,
            endpoints = endpoints.value.toVector,
            remotingEndpointName = if (bootstrapEnabled) Some(remotingEndpointName) else None,
            managementEndpointName = if (akkaManagementEnabled) Some(managementEndpointName) else None,
            privileged = privileged.value,
            environmentVariables = environmentVariables.value,
            version = Some(Keys.version.value),
            secrets = secrets.value,
            annotations = annotations.value,
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
        mappings += {
          val localPath = target.value / localName
          IO.write(localPath, readResource(localName))
          localPath.setExecutable(true)
          (localPath, startScriptLocation.value)
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

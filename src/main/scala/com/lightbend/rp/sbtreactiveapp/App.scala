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
import com.typesafe.sbt.packager.Keys.{ executableScriptName, stage }
import sbt._
import scala.collection.immutable.Seq

import Keys._

sealed trait App extends SbtReactiveAppKeys {
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
    super.projectSettings ++ magic.Lagom
      .version
      .toVector
      .map(v =>
        reactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom${SemVer.formatMajorMinor(v)}-java" -> true)
}

case object LagomScalaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ magic.Lagom
      .version
      .toVector
      .map(v => reactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom${SemVer.formatMajorMinor(v)}-scala" -> true)
}

case object LagomPlayJavaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ magic.Lagom
      .version
      .toVector
      .map(v =>
        reactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom${SemVer.formatMajorMinor(v)}-java" -> true)
}

case object LagomPlayScalaApp extends LagomApp {
  override def projectSettings: Seq[Setting[_]] =
    super.projectSettings ++ magic.Lagom
      .version
      .toVector
      .map(v => reactiveLibServiceDiscoveryProject := s"reactive-lib-service-discovery-lagom${SemVer.formatMajorMinor(v)}-scala" -> true)
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

case object BasicApp extends App {
  private val installReactiveSandbox = new java.util.concurrent.atomic.AtomicBoolean(false)
  private val reactiveSandboxInstalledLatch = new java.util.concurrent.CountDownLatch(1)

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

  def projectSettings: Seq[Setting[_]] =
    Vector(
      alpinePackages := Vector.empty,
      appName := name.value,
      appType := "basic",
      applications := Vector("default" -> Vector(s"bin/${executableScriptName.value}")),
      cpu := 0.0D,
      diskSpace := 0L,
      deployMinikubeEnableReactiveSandbox := {
        val kafkaEnabled = SettingKey[Boolean]("lagomKafkaEnabled").?.value.getOrElse(false)
        val cassandraEnabled = SettingKey[Boolean]("lagomCassandraEnabled").?.value.getOrElse(false)

        kafkaEnabled || cassandraEnabled
      },
      deployMinikubeReactiveSandboxExternalServices := Map(
        "cas_native" -> "_cql._tcp.reactive-sandbox-cassandra.default.svc.cluster.local",
        "kafka_native" -> "_broker._tcp.reactive-sandbox-kafka.default.svc.cluster.local",
        "elastic-search" -> "_http._tcp.reactive-sandbox-elasticsearch.default.svc.cluster.local"),
      deployMinikubeAdditionalExternalServices := Map.empty,
      deployMinikubeAkkaClusterBootstrapContactPoints := 1,
      deployMinikubePlayHostAllowedProperty := "play.filters.hosts.allowed.0",
      deployMinikubePlayHttpSecretKeyProperty := "play.http.secret.key",
      deployMinikubePlayHttpSecretKeyValue := "dev-minikube",
      deploy := {
        import complete.DefaultParsers._
        import scala.sys.process._

        val args = spaceDelimited("<arg>").parsed
        val isPlagom = Set("play", "lagom").contains(appType.value)
        val bootstrapEnabled = enableAkkaClusterBootstrap.value
        val reactiveSandbox = deployMinikubeEnableReactiveSandbox.value

        args.headOption.getOrElse("").trim.toLowerCase match {
          case "minikube" =>
            // @TODO Windows support is partially implemented. When finishing impl, remove this guard.
            // Issue that remains is that when arguments for rp have spaces, nodejs blows up

            if (isWindows) {
              sys.error("deploy is not currently supported on Microsoft Windows")
            }

            val minikubeExec =
              if (isWindows)
                target.value / "minikube-exec.ps1"
              else
                target.value / "minikube-exec"

            val log = streams.value.log
            val waitTimeMs = 1000 * 60 * 5

            cmd.minikube.assert()
            cmd.kubectl.assert()
            cmd.rp.assert()

            if (reactiveSandbox) {
              cmd.helm.assert();
            }

            // This wrapper script that sets minikube environment before execing its args
            // While it would be nice to do this all via the JVM, we need this mostly for hooking into
            // the sbt-native-packager building.

            IO.write(
              minikubeExec,

              if (isWindows)
                """|minikube docker-env | Invoke-Expression
                   |
                   |$cmd, $as = $args
                   |
                   |& $cmd $as
                   |""".stripMargin
              else
                """|#!/usr/bin/env bash
                   |
                   |set -e
                   |
                   |eval $(minikube docker-env --shell bash)
                   |
                   |exec "$@"
                   |""".stripMargin)

            assert(minikubeExec.setExecutable(true), s"Failed to mark $minikubeExec as executable")

            // We install the sandbox now (in on task via AtomicBoolean) but don't wait until after the build is
            // done for it to be deployed. This saves a bit of time for the user.

            val shouldInstallReactiveSandbox = reactiveSandbox && installReactiveSandbox.compareAndSet(false, true)

            if (shouldInstallReactiveSandbox) {
              if (!cmd.kubectl.deploymentExists("kube-system", "tiller-deploy")) {
                cmd.helm.init(log)

                cmd.kubectl.waitForDeployment(log, "kube-system", "tiller-deploy", waitTimeMs = waitTimeMs)
              }

              if (!cmd.kubectl.deploymentExists("default", "reactive-sandbox")) {
                cmd.helm.installReactiveSandbox(log)
              }
            }

            val minikubeIp = cmd.minikube.ip()

            val javaOpts =
              Vector(
                if (isPlagom) s"-D${deployMinikubePlayHostAllowedProperty.value}=$minikubeIp" else "",
                if (isPlagom) s"-D${deployMinikubePlayHttpSecretKeyProperty.value}=${deployMinikubePlayHttpSecretKeyValue.value}" else "")
                .filterNot(_.isEmpty)

            val services =
              if (reactiveSandbox)
                deployMinikubeReactiveSandboxExternalServices.value ++ deployMinikubeAdditionalExternalServices.value
              else
                deployMinikubeAdditionalExternalServices.value

            val serviceArgs =
              services.flatMap {
                case (serviceName, serviceAddress) =>
                  Vector("--external-service", s"$serviceName=$serviceAddress")
              }

            val rpArgs =
              Vector(
                dockerAlias.value.versioned,
                "--env",
                s"JAVA_OPTS=${javaOpts.mkString(" ")}") ++
                (if (bootstrapEnabled) Vector("--akka-cluster-skip-validation", "--pod-controller-replicas", deployMinikubeAkkaClusterBootstrapContactPoints.value.toString) else Vector.empty) ++
                serviceArgs ++
                deployMinikubeRpArguments.value

            publishLocalDocker(
              (stage in Docker).value,
              if (isWindows)
                "powershell.exe" +: minikubeExec.getAbsolutePath +: dockerBuildCommand.value
              else
                minikubeExec.getAbsolutePath +: dockerBuildCommand.value,
              log)

            log.info(s"Built image ${dockerAlias.value.versioned}")

            if (reactiveSandbox) {
              // FIXME: Make tiller & reactive-sandbox names configurable

              cmd.kubectl.waitForDeployment(log, "default", "reactive-sandbox", waitTimeMs = waitTimeMs)

              if (shouldInstallReactiveSandbox) {
                for {
                  pod <- cmd.kubectl.getPodNames("app=reactive-sandbox")
                  statement <- (deployMinikubeReactiveSandboxCqlStatements in ThisBuild).value
                } {
                  log.info(s"executing cassandra cql: $statement")

                  cmd.kubectl.invoke(log, Seq("exec", pod, "--", "/bin/bash", "-c", s"""/opt/cassandra/bin/cqlsh "$$POD_IP" -e "$statement""""))
                }

                reactiveSandboxInstalledLatch.countDown()
              } else {
                reactiveSandboxInstalledLatch.await()
              }
            }

            val kubernetesResourcesYaml = cmd.rp.generateKubernetesResources(minikubeExec.getAbsolutePath, log, rpArgs)

            cmd.kubectl.deleteAndApply(log, kubernetesResourcesYaml)

          case other =>
            sys.error(s"""Unknown deployment target: "$other". Available: minikube""")
        }
      },
      deployMinikubeRpArguments := Seq.empty,
      memory := 0L,
      enableCGroupMemoryLimit := true,
      privileged := false,
      environmentVariables := Map.empty,
      startScriptLocation := "/rp-start",
      secrets := Set.empty,
      reactiveLibVersion := "0.8.0",
      reactiveLibAkkaClusterBootstrapProject := "reactive-lib-akka-cluster-bootstrap" -> true,
      reactiveLibCommonProject := "reactive-lib-common" -> true,
      reactiveLibPlayHttpBindingProject := "reactive-lib-play-http-binding" -> true,
      reactiveLibSecretsProject := "reactive-lib-secrets" -> true,
      reactiveLibServiceDiscoveryProject := "reactive-lib-service-discovery" -> true,
      reactiveLibStatusProject := "reactive-lib-status" -> true,
      requiredAlpinePackages := Vector("bash"),
      enableAkkaClusterBootstrap := false,
      enableAkkaManagement := enableAkkaClusterBootstrap.value || enableStatus.value,
      enableCommon := true,
      enablePlayHttpBinding := false,
      enableSecrets := secrets.value.nonEmpty,
      enableServiceDiscovery := enableAkkaClusterBootstrap.value,
      enableStatus := enableAkkaClusterBootstrap.value,

      prependRpConf := "application.conf",

      akkaClusterBootstrapEndpointName := "akka-remote",

      akkaClusterBootstrapSystemName := "",

      akkaManagementEndpointName := "akka-mgmt-http",

      httpIngressHosts := Seq.empty,

      httpIngressPaths := Seq.empty,

      httpIngressPorts := Seq(80, 443),

      unmanagedResources in Compile := {
        val base = (unmanagedResources in Compile).value
        val baseDest = (target in Compile).value
        val dependencyClasspathValue = (dependencyClasspath in Compile).value
        val prependRpConfValue = prependRpConf.value

        if (prependRpConfValue.isEmpty)
          base
        else
          magic
            .Build
            .extractApplicationConf(Vector(ToolingConfig), Vector(prependRpConfValue), base, dependencyClasspathValue)
            .fold(base) { mergedConfig =>
              val dest = baseDest / LocalApplicationConfig

              IO.write(dest, mergedConfig)

              base :+ dest
            }
      },

      allDependencies :=
        allDependencies.value ++
        lib(scalaVersion.value, reactiveLibAkkaClusterBootstrapProject.value, reactiveLibVersion.value, enableAkkaClusterBootstrap.value) ++
        lib(scalaVersion.value, reactiveLibCommonProject.value, reactiveLibVersion.value, enableCommon.value) ++
        lib(scalaVersion.value, reactiveLibPlayHttpBindingProject.value, reactiveLibVersion.value, enablePlayHttpBinding.value) ++
        lib(scalaVersion.value, reactiveLibSecretsProject.value, reactiveLibVersion.value, enableSecrets.value) ++
        lib(scalaVersion.value, reactiveLibServiceDiscoveryProject.value, reactiveLibVersion.value, enableServiceDiscovery.value) ++
        lib(scalaVersion.value, reactiveLibStatusProject.value, reactiveLibVersion.value, enableStatus.value),

      endpoints := {
        val clusterEndpointName = akkaClusterBootstrapEndpointName.value
        val managementEndpointName = akkaManagementEndpointName.value
        val bootstrapEnabled = enableAkkaClusterBootstrap.value
        val managementEnabled = enableAkkaManagement.value

        endpoints.?.value.getOrElse(Seq.empty) ++
          (if (bootstrapEnabled) Seq(TcpEndpoint(clusterEndpointName)) else Seq.empty) ++
          (if (managementEnabled) Seq(TcpEndpoint(managementEndpointName)) else Seq.empty)
      },

      javaOptions in SbtNativePackager.Universal ++= (
        if (memory.value > 0L && enableCGroupMemoryLimit.value)
          Vector("-J-XX:+UnlockExperimentalVMOptions", "-J-XX:+UseCGroupMemoryLimitForHeap")
        else
          Vector.empty),

      dockerEntrypoint := (
        if (startScriptLocation.value.isEmpty)
          dockerEntrypoint.value
        else
          startScriptLocation.value +: dockerEntrypoint.value),

      dockerBaseImage := "openjdk:8-jre-alpine",

      dockerEntrypoint := Vector.empty,

      dockerCommands := {
        val addCommand = Some(startScriptLocation.value)
          .filter(_.nonEmpty)
          .toVector
          .map(path => docker.Cmd("COPY", localName, path))

        val bootstrapEnabled = enableAkkaClusterBootstrap.value
        val bootstrapSystemName = Some(akkaClusterBootstrapSystemName.value).filter(_.nonEmpty && bootstrapEnabled)
        val commonEnabled = enableCommon.value
        val playHttpBindingEnabled = enablePlayHttpBinding.value
        val secretsEnabled = enableSecrets.value
        val serviceDiscoveryEnabled = enableServiceDiscovery.value
        val statusEnabled = enableStatus.value
        val akkaManagementEnabled = bootstrapEnabled || statusEnabled
        val rawDockerCommands = dockerCommands.value
        val alpinePackagesValue = alpinePackages.value
        val requiredAlpinePackagesValue = requiredAlpinePackages.value
        val allAlpinePackages = (alpinePackagesValue ++ requiredAlpinePackagesValue).distinct.sorted

        val dockerWithPackagesCommands =
          if (rawDockerCommands.isEmpty || allAlpinePackages.isEmpty)
            rawDockerCommands
          else
            rawDockerCommands.head +:
              docker.Cmd("RUN", Vector("/sbin/apk", "add", "--no-cache") ++ allAlpinePackages: _*) +:
              rawDockerCommands.tail

        dockerWithPackagesCommands ++ addCommand ++ labelCommand(SbtReactiveApp
          .labels(
            appName = Some(appName.value),
            appType = Some(appType.value),
            applications = applications.value.toVector.map {
              case (aName, appValue) =>
                val script =
                  startScriptLocation.value

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
            privileged = privileged.value,
            environmentVariables = environmentVariables.value,
            version = Some(Keys.version.value),
            secrets = secrets.value,
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

object App {
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

private object SemVer {
  def formatMajorMinor(version: String): String = version.filterNot(_ == '.').take(2)

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

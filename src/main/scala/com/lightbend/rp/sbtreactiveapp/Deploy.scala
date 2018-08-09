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

import com.lightbend.rp.sbtreactiveapp.BasicApp._
import com.lightbend.rp.sbtreactiveapp.SbtReactiveAppPlugin.localImport._
import com.lightbend.rp.sbtreactiveapp.SbtReactiveAppPlugin._
import com.typesafe.sbt.SbtNativePackager
import com.typesafe.sbt.packager.docker
import com.typesafe.sbt.packager.docker.DockerPlugin.{ publishDocker, publishLocalDocker }
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{ dockerAlias, dockerBuildCommand }
import com.typesafe.sbt.packager.Keys.{ daemonGroup, daemonUser, executableScriptName, stage }
import sbt._

import scala.collection.immutable.Seq
import com.typesafe.sbt.packager.docker.DockerSupport
import Keys._

trait DeployableApp extends App {
  private val installReactiveSandbox = new java.util.concurrent.atomic.AtomicBoolean(false)
  private val reactiveSandboxInstalledLatch = new java.util.concurrent.CountDownLatch(1)

  def projectSettings: Seq[Setting[_]] = Vector(
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
        case "minikube" => {
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
        }

        case other =>
          sys.error(s"""Unknown deployment target: "$other". Available: minikube""")
      }
    },
    deployMinikubeRpArguments := Seq.empty)
}
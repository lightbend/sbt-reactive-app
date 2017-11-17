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

import sbt._
import scala.collection.immutable.Seq

trait SbtReactiveAppKeys {
  val diskSpace = SettingKey[Option[Long]]("rp-disk-space")

  val memory = SettingKey[Option[Long]]("rp-memory")

  val nrOfCpus = SettingKey[Option[Double]]("rp-nr-of-cpus")

  val endpoints = TaskKey[Seq[Endpoint]]("rp-endpoints")

  val volumes = TaskKey[Map[String, Volume]]("rp-volumes")

  val privileged = SettingKey[Boolean]("rp-privileged")

  val healthCheck = TaskKey[Option[Check]]("rp-health-check")

  val readinessCheck = TaskKey[Option[Check]]("rp-readiness-check")

  val akkaClusterBootstrapEndpointName = SettingKey[String]("rp-akka-cluster-bootstrap-endpoint-name")

  val lagomIngressHosts = SettingKey[Seq[String]]("rp-lagom-ingress-hosts")

  val lagomIngressPorts = SettingKey[Seq[Int]]("rp-lagom-ingress-ports")

  val environmentVariables = SettingKey[Map[String, EnvironmentVariable]]("rp-environment-variables")

  val enableAkkaClusterBootstrap = SettingKey[Option[Boolean]]("rp-enable-akka-cluster-bootstrap", "Include Akka Cluster Bootstrapping. By default, included if a Lagom persistence module is defined.")

  val enableCommon = SettingKey[Boolean]("rp-enable-common")

  val enablePlayHttpBinding = SettingKey[Boolean]("rp-enable-play-http-binding")

  val enableSecrets = SettingKey[Option[Boolean]]("rp-enable-secrets", "Include Secrets API. By default, included if any secrets are defined.")

  val enableServiceDiscovery = SettingKey[Boolean]("rp-enable-service-discovery")

  val reactiveLibAkkaClusterBootstrapProject = SettingKey[(String, Boolean)]("rp-reactive-lib-akka-cluster-bootstrap-project")

  val reactiveLibCommonProject = SettingKey[(String, Boolean)]("rp-reactive-lib-common-project")

  val reactiveLibPlayHttpBindingProject = SettingKey[(String, Boolean)]("rp-reactive-lib-play-http-binding-project")

  val reactiveLibSecretsProject = SettingKey[(String, Boolean)]("rp-reactive-lib-secrets-project")

  val reactiveLibServiceDiscoveryProject = SettingKey[(String, Boolean)]("rp-reactive-lib-service-discovery-project")

  val reactiveLibVersion = SettingKey[String]("rp-reactive-lib-version")

  val startScriptLocation = SettingKey[Option[String]]("rp-start-script")

  val secrets = SettingKey[Set[Secret]]("rp-secrets")

  private[sbtreactiveapp] val akkaClusterBootstrapEnabled = TaskKey[Boolean]("rp-akka-cluster-bootstrap-enabled")

  private[sbtreactiveapp] val secretsEnabled = TaskKey[Boolean]("rp-secrets-enabled")
}

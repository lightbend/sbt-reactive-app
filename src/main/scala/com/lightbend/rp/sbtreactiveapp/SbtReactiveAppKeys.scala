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

trait SbtReactiveAppKeys {
  /**
   * Defines the application name. This defaults to the project name and is used in naming services. On Kubernetes,
   * this gets used for the `Service` name (after being scrubbed according to service name logic)
   */
  val appName = TaskKey[String]("rp-app-name")

  /**
   * Defines the application type - this is currently one of lagom, play, or basic. It's used internally and not
   * intended to be overridden.
   */
  val appType = SettingKey[String]("rp-app-type")

  /**
   * Defines the optional disk space requirement for scheduling this application.
   */
  val diskSpace = SettingKey[Option[Long]]("rp-disk-space")

  /**
   * Defines the optional memory requirement for scheduling this application.
   */
  val memory = SettingKey[Option[Long]]("rp-memory")

  /**
   * Defines the optional CPU share requirement for scheduling this application. This follows Mesos conventions, so
   * for CGroup shares this value is multiplied by 1024. A reasonable starting value is 0.1.
   */
  val nrOfCpus = SettingKey[Option[Double]]("rp-nr-of-cpus")

  /**
   * Defines the endpoints for this application. On Kubernetes, Services will be created for each endpoint, and if
   * the endpoint specifies Ingress then it will be generated as well.
   */
  val endpoints = TaskKey[Seq[Endpoint]]("rp-endpoints")

  /**
   * Defines the volumes for this application. This is a mapping from the guest path to the details for the volume.
   */
  val volumes = TaskKey[Map[String, Volume]]("rp-volumes")

  /**
   * If true, the container should be run in a privileged setting, i.e. with root privileges.
   */
  val privileged = SettingKey[Boolean]("rp-privileged")

  /**
   * Defines a health check for this application. If the health check fails, the application will be scheduled
   * elsewhere.
   */
  val healthCheck = TaskKey[Option[Check]]("rp-health-check")

  /**
   * Defines a readiness check for this application. The application will not be available for service discovery
   * until it passes its readiness check.
   */
  val readinessCheck = TaskKey[Option[Check]]("rp-readiness-check")

  /**
   * Defines the endpoint name for the akka remoting port. reactive-lib expects the default values here so you
   * should not have to change this under normal circumstances.
   */
  val akkaClusterBootstrapEndpointName = SettingKey[String]("rp-akka-cluster-bootstrap-endpoint-name")

  /**
   * Defines the endpoint name for the akka cluster management port. reactive-lib expects the default values here so you
   * should not have to change this under normal circumstances.
   */
  val akkaClusterBootstrapManagementEndpointName = SettingKey[String]("rp-akka-cluster-bootstrap-management-endpoint-name")

  /**
   * For endpoints that are autopopulated, they will declare ingress for these hosts. That is, they'll be available
   * on the public nodes or ingress controllers at these hostnames. Defaults to nothing for Basic apps, "/" for Play
   * apps, and the collection of service endpoints for Lagom apps.
   */
  val httpIngressHosts = SettingKey[Seq[String]]("rp-ingress-hosts")

  /**
   * For endpoints that are autopopulated, they will declare ingress for these hosts. That is, they'll be available
   * on the public nodes or ingress controllers at these hostnames.
   */
  val httpIngressPaths = TaskKey[Seq[String]]("rp-ingress-paths")

  /**
   * For endpoints that are autopopulated, they will declare ingress for these posts. That is, they'll be available
   * on the public nodes or ingress controllers at these posts. Defaults to 80 and 443.
   */
  val httpIngressPorts = SettingKey[Seq[Int]]("rp-ingress-ports")

  /**
   * Defines environment variable values that this application should be run with at runtime.
   */
  val environmentVariables = SettingKey[Map[String, EnvironmentVariable]]("rp-environment-variables")

  /**
   * Enables Akka Cluster Bootstraping (reactive-lib). To auto detect if it is needed, set to None,
   * otherwise set to Some(true) to enable or Some(false) to disable.
   */
  val enableAkkaClusterBootstrap = SettingKey[Option[Boolean]]("rp-enable-akka-cluster-bootstrap", "Include Akka Cluster Bootstrapping. By default, included if a Lagom persistence module is defined.")

  /**
   * Enables the common library (reactive-lib). This defaults to true. It provides a few APIs that the application
   * is using to determine what target platform its running on, what ports it should bind on, etc.
   */
  val enableCommon = SettingKey[Boolean]("rp-enable-common")

  /**
   * Enable the Play HTTP binding library (reactive-lib). This is enabled by default for Lagom and Play projects and
   * allows the tooling to automatically assign ports.
   */
  val enablePlayHttpBinding = SettingKey[Boolean]("rp-enable-play-http-binding")

  /**
   * Enable the secret library (reactive-lib). If None (default), it will automatically be enabled if any secrets are
   * declared. If Some(value), it will be forced on (true) or off (false)
   */
  val enableSecrets = SettingKey[Option[Boolean]]("rp-enable-secrets", "Include Secrets API. By default, included if any secrets are defined.")

  /**
   * Enables the service discovery library (reactive-lib). If enabled, a service locator API will be on the classpath
   * and for Lagom projects, an implementation of Lagom's service locator will be provided.
   */
  val enableServiceDiscovery = SettingKey[Boolean]("rp-enable-service-discovery")

  /**
   * If defined (default: Some("rp-tooling.conf")), all resources with the given name will be prepended to the
   * unmanaged application.conf file, or one will be created if none exists. To disable this behavior, specify `None`.
   */
  val prependRpConf = SettingKey[Option[String]]("rp-prepend-rp-application-conf")

  val reactiveLibAkkaClusterBootstrapProject = SettingKey[(String, Boolean)]("rp-reactive-lib-akka-cluster-bootstrap-project")

  val reactiveLibCommonProject = SettingKey[(String, Boolean)]("rp-reactive-lib-common-project")

  val reactiveLibPlayHttpBindingProject = SettingKey[(String, Boolean)]("rp-reactive-lib-play-http-binding-project")

  val reactiveLibSecretsProject = SettingKey[(String, Boolean)]("rp-reactive-lib-secrets-project")

  val reactiveLibServiceDiscoveryProject = SettingKey[(String, Boolean)]("rp-reactive-lib-service-discovery-project")

  /**
   * Defines the published reactive-lib version to include in the project. You can set this value to upgrade
   * reactive-lib without having to update sbt-reactive-app.
   */
  val reactiveLibVersion = SettingKey[String]("rp-reactive-lib-version")

  val startScriptLocation = SettingKey[Option[String]]("rp-start-script")

  /**
   * Defines secrets that will be made available to the application at runtime. The secrets API in reactive-lib
   * can then be used to decode these secrets in a consistent and platform-independent manner.
   */
  val secrets = SettingKey[Set[Secret]]("rp-secrets")

  private[sbtreactiveapp] val akkaClusterBootstrapEnabled = TaskKey[Boolean]("rp-akka-cluster-bootstrap-enabled")

  private[sbtreactiveapp] val lagomRawEndpoints = TaskKey[Seq[Endpoint]]("rp-lagom-raw-endpoints")

  private[sbtreactiveapp] val secretsEnabled = TaskKey[Boolean]("rp-secrets-enabled")
}

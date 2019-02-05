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
import com.typesafe.sbt.packager.docker.CmdLike
import com.typesafe.config.Config

trait SbtReactiveAppKeys {
  /**
   * Defines the application name. This defaults to the project name and is used in naming services. On Kubernetes,
   * this gets used for the `Service` name (after being scrubbed according to service name logic)
   */
  val rpAppName = taskKey[String]("Defines the application name.")

  /**
   * Defines the application type - this is currently one of lagom, play, or basic. It's used internally and not
   * intended to be overridden.
   */
  val rpAppType = settingKey[String]("Internal. Defines the application type - this is currently one of lagom, play, or basic.")

  /**
   * Defines the optional disk space requirement for scheduling this application.
   */
  val rpDiskSpace = settingKey[Long]("Defines the optional disk space requirement for scheduling this application.")

  /**
   * Defines the optional memory requirement for scheduling this application. Defaults to 0, i.e. disabled.
   */
  val rpMemory = settingKey[Long]("Defines the optional memory requirement for scheduling this application. Defaults to 0, i.e. disabled.")

  /**
   * If true, `rpMemory` setting will set CGroup limits for the JVM in addition to platform (eg. Kubernetes) limits.
   * If false, only platform limits will be enforced. Default is true.
   */
  val rpEnableCGroupMemoryLimit = settingKey[Boolean]("If true, `rpMemory` setting will set CGroup limits for the JVM in addition to platform (eg. Kubernetes) limits.")

  /**
   * Defines the optional CPU share requirement for scheduling this application. This follows Mesos conventions, so
   * for CGroup shares this value is multiplied by 1024. A reasonable starting value is 0.1. Defaults to 0, i.e.
   * disabled.
   */
  val rpCpu = settingKey[Double]("""Defines the optional CPU share requirement for scheduling this application.
                                   |This follows Mesos conventions, so for CGroup shares this value is multiplied by 1024.
                                   |A reasonable starting value is 0.1. Defaults to 0, i.e. disabled.""".stripMargin)

  /**
   * Defines the endpoints for this application. On Kubernetes, Services will be created for each endpoint, and if
   * the endpoint specifies Ingress then it will be generated as well.
   */
  val rpEndpoints = taskKey[Seq[Endpoint]]("""Defines the endpoints for this application. On Kubernetes, Services will be created for each endpoint,
                                              and if the endpoint specifies Ingress then it will be generated as well.""".stripMargin)

  /**
   * If true, the container should be run in a privileged setting, i.e. with root privileges.
   */
  val rpPrivileged = settingKey[Boolean]("If true, the container should be run in a privileged setting.")

  /**
   * Defines the endpoint name for the akka remoting port. reactive-lib expects the default values here so you
   * should not have to change this under normal circumstances.
   */
  val rpAkkaClusterBootstrapEndpointName = settingKey[String]("Defines the endpoint name for the akka remoting port.")

  /**
   * If specified, app will join other nodes that specify this same system name. This can be used to allow different
   * applications to join the same cluster. If empty (default), the default logic of using the appName will be
   * used instead.
   */
  val rpAkkaClusterBootstrapSystemName = settingKey[String]("If specified, app will join other nodes that specify this same system name.")

  /**
   * Defines the endpoint name for the akka management port. reactive-lib expects the default values here so you
   * should not have to change this under normal circumstances.
   */
  val rpAkkaManagementEndpointName = settingKey[String]("Defines the endpoint name for the akka management port.")

  lazy val rpPackagingDockerCommmands = settingKey[Seq[CmdLike]]("Docker commands related to packaing.")

  lazy val rpPermissionsDockerCommmands = settingKey[Seq[CmdLike]]("Docker commands related to file permissions.")

  lazy val rpApplicationConfig = taskKey[Config]("Used internally for configuration detetion")

  /**
   * Defines packages to install on top of the base alpine image.
   */
  @deprecated("""Use rpPackagingDockerCommmands:
                |import com.typesafe.sbt.packager.docker._
                |rpPackagingDockerCommmands := Vector(
                |  Cmd("RUN", "/sbin/apk", "add", "--no-cache", "bash", "coreutils"))""".stripMargin, "1.4.1")
  val rpAlpinePackages = settingKey[Seq[String]]("")

  /**
   * Defines the available applications. This is a list of appName -> appArgs. The operator can specify alternate
   * applications to dynamically override the command/arguments that are executed.
   */
  val rpApplications = taskKey[Seq[(String, Seq[String])]]("Defines the available applications.")

  /**
   * This task deploys all aggregated projects into a target environment. Currently, minikube is supported.
   */
  val rpDeploy = inputKey[Unit]("Deploys all aggregated projects into a target environment. Currently, minikube is supported.")

  /**
   * A map of service names to service lookup addresses. This will be provided as an argument to rp for resources
   * are are generated when running deploy minikube. Note that this map will only be added if reactive sandbox is
   * enabled.
   */
  val rpDeployMinikubeReactiveSandboxExternalServices = settingKey[Map[String, String]]("A map of service names to service lookup addresses.")

  /**
   * An additional map of service names to service lookup addresses. These will always be provided to rp and take
   * precedence over the Reactive Sandbox addresses.
   */
  val rpDeployMinikubeAdditionalExternalServices = settingKey[Map[String, String]]("An additional map of service names to service lookup addresses.")

  /**
   * When deploying applications with Akka Cluster Bootstrap enabled, the services will initially be started with this
   * many contact points / replicas.
   */
  val rpDeployMinikubeAkkaClusterBootstrapContactPoints = settingKey[Int]("When deploying applications with Akka Cluster Bootstrap enabled, the services will initially be started with this many contact points / replicas.")

  /**
   * If enabled, Reactive Sandbox (a Docker image containing Cassandra, Kafka, ZooKeeper, Elasticsearch) will be
   * deployed with this app.
   */
  val rpDeployMinikubeEnableReactiveSandbox = settingKey[Boolean]("If enabled, Reactive Sandbox (a Docker image containing Cassandra, Kafka, ZooKeeper, Elasticsearch) will be deployed with this app.")

  /**
   * If deploying a Play application, this property will be set to the Minikube IP.
   */
  val rpDeployMinikubePlayHostAllowedProperty = settingKey[String]("If deploying a Play application, this property will be set to the Minikube IP.")

  /**
   * If deploying a Play application, this property will be set to the value specified below.
   */
  val rpDeployMinikubePlayHttpSecretKeyProperty = settingKey[String]("If deploying a Play application, this property will be set to the value specified below.")

  /**
   * If deploying a Play application, this property will be set to the value specified above.
   */
  val rpDeployMinikubePlayHttpSecretKeyValue = settingKey[String]("If deploying a Play application, this property will be set to the value specified above.")

  /**
   * Set this setting (build-wide, i.e. deployMinikubeReactiveSandboxCqlStatements in ThisBuild := ...) to a sequence
   * of CQL statements that should be executed against Cassandra when the Reactive Sandbox is installed.
   */
  val rpDeployMinikubeReactiveSandboxCqlStatements = settingKey[Seq[String]]("")

  /**
   * Additional arguments to invoke rp with for this app.
   */
  val rpDeployMinikubeRpArguments = settingKey[Seq[String]]("")

  /**
   * For endpoints that are autopopulated, they will declare ingress for these hosts. That is, they'll be available
   * on the public nodes or ingress controllers at these hostnames. Defaults to nothing for Basic apps, "/" for Play
   * apps, and the collection of service endpoints for Lagom apps.
   */
  val rpHttpIngressHosts = settingKey[Seq[String]]("For endpoints that are autopopulated, they will declare ingress for these hosts.")

  /**
   * For endpoints that are autopopulated, they will declare ingress for these hosts. That is, they'll be available
   * on the public nodes or ingress controllers at these hostnames.
   */
  val rpHttpIngressPaths = taskKey[Seq[String]]("For endpoints that are autopopulated, they will declare ingress for these hosts.")

  /**
   * For endpoints that are autopopulated, they will declare ingress for these ports. That is, they'll be available
   * on the public nodes or ingress controllers at these ports. Defaults to 80 and 443.
   */
  val rpHttpIngressPorts = settingKey[Seq[Int]]("For endpoints that are autopopulated, they will declare ingress for these ports.")

  /**
   * Defines environment variable values that this application should be run with at runtime.
   */
  val rpEnvironmentVariables = settingKey[Map[String, EnvironmentVariable]]("Defines environment variable values that this application should be run with at runtime.")

  /**
   * Enables Akka Cluster Bootstrapping (reactive-lib).
   */
  val rpEnableAkkaClusterBootstrap = taskKey[Boolean]("Include Akka Cluster Bootstrapping. By default, included if a Lagom persistence module is defined.")

  /**
   * Enables Akka Management (reactive-lib).
   *
   * By default, this is enabled if one of the following modules is enabled:
   *   - akka-cluster-bootstrap
   *   - status
   */
  val rpEnableAkkaManagement = taskKey[Boolean]("")

  /**
   * Enables the common library (reactive-lib). This defaults to true. It provides a few APIs that the application
   * is using to determine what target platform its running on, what ports it should bind on, etc.
   */
  val rpEnableCommon = taskKey[Boolean]("Enables the common library (reactive-lib).")

  /**
   * Enable the secret library (reactive-lib). By default, it will automatically be enabled if any secrets are
   * declared.
   */
  val rpEnableSecrets = taskKey[Boolean]("Include Secrets API. By default, included if any secrets are defined.")

  /**
   * Enables the service discovery library (reactive-lib). If enabled, a service locator API will be on the classpath
   * and for Lagom projects, an implementation of Lagom's service locator will be provided.
   */
  val rpEnableServiceDiscovery = taskKey[Boolean]("Enables the service discovery library (reactive-lib).")

  /**
   * Enables the status library (reactive-lib). By default, it will automatically be enabled if any modules
   * that define health/readiness checks are enabled. Currently, this is only `akka-cluster-bootstrap`. At runtime,
   * routes for health and readiness will be added to the Akka Management HTTP server, and at resource generation
   * the appropriate health/readiness configuration will be generated to monitor these endpoints.
   */
  val rpEnableStatus = taskKey[Boolean]("Enables the status library (reactive-lib).")

  /**
   * Executes `helm` program with any specified arguments. This may be expanded to support auto completion in
   * the future.
   */
  val rpHelm = inputKey[Unit]("Executes `helm` program with any specified arguments.")

  /**
   * Executes `kubectl` program with any specified arguments. This may be expanded to support auto completion in
   * the future.
   */
  val rpKubectl = inputKey[Unit]("Executes `kubectl` program with any specified arguments.")

  /**
   * Executes `minikube` program with any specified arguments. This may be expanded to support auto completion in
   * the future.
   */
  val rpMinikube = inputKey[Unit]("Executes `minikube` program with any specified arguments.")

  /**
   * If non-empty (default: "application.conf"), all unmanaged resources with the given name, in any dependencies,
   * as well as all managed resources named "rp-tooling.conf'",
   * will be concatenated into a managed resource "rp-application.conf" file. To disable this behavior, specify "".
   */
  val rpPrependRpConf = settingKey[String]("")

  /**
   * Defines the published reactive-lib version to include in the project. You can set this value to upgrade
   * reactive-lib without having to update sbt-reactive-app.
   */
  val rpReactiveLibVersion = settingKey[String]("Defines the published reactive-lib version to include in the project.")

  /**
   * Defines location where the wrapper script for app execution should be placed (in the container). If empty,
   * no wrapper script is used.
   */
  private[sbtreactiveapp] val rpStartScriptLocation = settingKey[String]("")

  /**
   * Defines secrets that will be made available to the application at runtime. The secrets API in reactive-lib
   * can then be used to decode these secrets in a consistent and platform-independent manner.
   */
  val rpSecrets = settingKey[Set[Secret]]("Defines secrets that will be made available to the application at runtime.")

  /**
   * Annotations to attach to the application at runtime. On Kubernetes, this will become a pod annotation. On
   * DC/OS it will be a label.
   */
  val rpAnnotations = taskKey[Map[String, String]](
    "Annotations to attach to the application at runtime. On Kubernetes, this will become a pod annotation. " +
      "On DC/OS it will be a label.")

  private[sbtreactiveapp] val rpDeployMinikubeDockerEnv = taskKey[String]("")

  private[sbtreactiveapp] val rpLagomRawEndpoints = taskKey[Seq[Endpoint]]("")

  private[sbtreactiveapp] val rpReactiveLibAkkaClusterBootstrapProject = settingKey[(String, Boolean)]("")
  private[sbtreactiveapp] val rpReactiveLibCommonProject = settingKey[(String, Boolean)]("")
  private[sbtreactiveapp] val rpReactiveLibSecretsProject = settingKey[(String, Boolean)]("")
  private[sbtreactiveapp] val rpReactiveLibServiceDiscoveryProject = settingKey[(String, Boolean)]("")
  private[sbtreactiveapp] val rpReactiveLibStatusProject = settingKey[(String, Boolean)]("")

  /**
   * Defines alpine packages that are installed (and required) by this plugin.
   * This is combined with the user-defined packages (`alpinePackages`).
   */
  private[sbtreactiveapp] val rpRequiredAlpinePackages = settingKey[Seq[String]]("")

  @deprecated("use rpAppName", "1.7.0") val appName = rpAppName
  @deprecated("use rpAppType", "1.7.0") val appType = rpAppType
  @deprecated("use rpDiskSpace", "1.7.0") val diskSpace = rpDiskSpace
  @deprecated("use rpMemory", "1.7.0") val memory = rpMemory

  @deprecated("use rpEnableCGroupMemoryLimit", "1.7.0")
  val enableCGroupMemoryLimit = rpEnableCGroupMemoryLimit

  @deprecated("use rpCpu", "1.7.0") val cpu = rpCpu
  @deprecated("use rpEndpoints", "1.7.0") val endpoints = rpEndpoints
  @deprecated("use rpPrivileged", "1.7.0") val privileged = rpPrivileged

  @deprecated("use rpAkkaClusterBootstrapEndpointName", "1.7.0")
  val akkaClusterBootstrapEndpointName = rpAkkaClusterBootstrapEndpointName

  @deprecated("use rpAkkaClusterBootstrapSystemName", "1.7.0")
  val akkaClusterBootstrapSystemName = rpAkkaClusterBootstrapSystemName

  @deprecated("use rpAkkaManagementEndpointName", "1.7.0")
  val akkaManagementEndpointName = rpAkkaManagementEndpointName

  @deprecated("use rpAlpinePackages", "1.7.0") val alpinePackages = rpAlpinePackages
  @deprecated("use rpApplications", "1.7.0") val applications = rpApplications
  @deprecated("use rpDeploy", "1.7.0") val deploy = rpDeploy

  @deprecated("use rpDeployMinikubeReactiveSandboxExternalServices", "1.7.0")
  val deployMinikubeReactiveSandboxExternalServices = rpDeployMinikubeReactiveSandboxExternalServices

  @deprecated("use rpDeployMinikubeAdditionalExternalServices", "1.7.0")
  val deployMinikubeAdditionalExternalServices = rpDeployMinikubeAdditionalExternalServices

  @deprecated("use rpDeployMinikubeAkkaClusterBootstrapContactPoints", "1.7.0")
  val deployMinikubeAkkaClusterBootstrapContactPoints = rpDeployMinikubeAkkaClusterBootstrapContactPoints

  @deprecated("use rpDeployMinikubeEnableReactiveSandbox", "1.7.0")
  val deployMinikubeEnableReactiveSandbox = rpDeployMinikubeEnableReactiveSandbox

  @deprecated("use rpDeployMinikubePlayHostAllowedProperty", "1.7.0")
  val deployMinikubePlayHostAllowedProperty = rpDeployMinikubePlayHostAllowedProperty

  @deprecated("use rpDeployMinikubePlayHttpSecretKeyProperty", "1.7.0")
  val deployMinikubePlayHttpSecretKeyProperty = rpDeployMinikubePlayHttpSecretKeyProperty

  @deprecated("use rpDeployMinikubePlayHttpSecretKeyValue", "1.7.0")
  val deployMinikubePlayHttpSecretKeyValue = rpDeployMinikubePlayHttpSecretKeyValue

  @deprecated("use rpDeployMinikubeReactiveSandboxCqlStatements", "1.7.0")
  val deployMinikubeReactiveSandboxCqlStatements = rpDeployMinikubeReactiveSandboxCqlStatements

  @deprecated("use rpDeployMinikubeRpArguments", "1.7.0")
  val deployMinikubeRpArguments = rpDeployMinikubeRpArguments

  @deprecated("use rpHttpIngressHosts", "1.7.0") val httpIngressHosts = rpHttpIngressHosts
  @deprecated("use rpHttpIngressPaths", "1.7.0") val httpIngressPaths = rpHttpIngressPaths
  @deprecated("use rpHttpIngressPorts", "1.7.0") val httpIngressPorts = rpHttpIngressPorts
  @deprecated("use rpEnvironmentVariables", "1.7.0") val environmentVariables = rpEnvironmentVariables

  @deprecated("use rpEnableAkkaClusterBootstrap", "1.7.0")
  val enableAkkaClusterBootstrap = rpEnableAkkaClusterBootstrap

  @deprecated("use rpEnableAkkaManagement", "1.7.0")
  val enableAkkaManagement = rpEnableAkkaManagement

  @deprecated("use rpEnableCommon", "1.7.0")
  val enableCommon = rpEnableCommon

  @deprecated("use rpEnableSecrets", "1.7.0")
  val enableSecrets = rpEnableSecrets

  @deprecated("use rpEnableServiceDiscovery", "1.7.0")
  val enableServiceDiscovery = rpEnableServiceDiscovery

  @deprecated("use rpEnableStatus", "1.7.0")
  val enableStatus = rpEnableStatus

  @deprecated("use rpHelm", "1.7.0") val helm = rpHelm
  @deprecated("use rpKubectl", "1.7.0") val kubectl = rpKubectl
  @deprecated("use rpMinikube", "1.7.0") val minikube = rpMinikube
  @deprecated("use rpPrependRpConf", "1.7.0") val prependRpConf = rpPrependRpConf
  @deprecated("use rpReactiveLibVersion", "1.7.0") val reactiveLibVersion = rpReactiveLibVersion
  @deprecated("use rpSecrets", "1.7.0") val secrets = rpSecrets
  @deprecated("use rpAnnotations", "1.7.0") val annotations = rpAnnotations
}

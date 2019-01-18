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
  val appName = TaskKey[String]("rp-app-name")

  /**
   * Defines the application type - this is currently one of lagom, play, or basic. It's used internally and not
   * intended to be overridden.
   */
  val appType = SettingKey[String]("rp-app-type")

  /**
   * Defines the optional disk space requirement for scheduling this application.
   */
  val diskSpace = SettingKey[Long]("rp-disk-space")

  /**
   * Defines the optional memory requirement for scheduling this application. Defaults to 0, i.e. disabled.
   */
  val memory = SettingKey[Long]("rp-memory")

  /**
   * If true, `memory` setting will set CGroup limits for the JVM in addition to platform (eg. Kubernetes) limits.
   * If false, only platform limits will be enforced. Default is true.
   */
  val enableCGroupMemoryLimit = SettingKey[Boolean]("rp-enable-cgroup-memory-limit")

  /**
   * Defines the optional CPU share requirement for scheduling this application. This follows Mesos conventions, so
   * for CGroup shares this value is multiplied by 1024. A reasonable starting value is 0.1. Defaults to 0, i.e.
   * disabled.
   */
  val cpu = SettingKey[Double]("rp-cpu")

  /**
   * Defines the endpoints for this application. On Kubernetes, Services will be created for each endpoint, and if
   * the endpoint specifies Ingress then it will be generated as well.
   */
  val endpoints = TaskKey[Seq[Endpoint]]("rp-endpoints")

  /**
   * If true, the container should be run in a privileged setting, i.e. with root privileges.
   */
  val privileged = SettingKey[Boolean]("rp-privileged")

  /**
   * Run application as the given user. Defaults to `daemon`.
   */
  val runAsUser = SettingKey[String]("rp-run-as-user", "Run application as the given user. Defaults to `daemon`.")

  /**
   * Group that the user belongs to. Defaults to the user name.
   */
  val runAsUserGroup = SettingKey[String]("rp-run-as-user-group", "Group that the user belongs to. Defaults to the user name.")

  /**
   * UID of the user.
   */
  val runAsUserUID = SettingKey[Int]("rp-run-as-user-uid", "UID of the user.")

  /**
   * GID of the user's group.
   */
  val runAsUserGID = SettingKey[Int]("rp-run-as-user-gid", "GID of the user's group.")

  /**
   * Defines the endpoint name for the akka remoting port. reactive-lib expects the default values here so you
   * should not have to change this under normal circumstances.
   */
  val akkaClusterBootstrapEndpointName = SettingKey[String]("rp-akka-cluster-bootstrap-endpoint-name")

  /**
   * If specified, app will join other nodes that specify this same system name. This can be used to allow different
   * applications to join the same cluster. If empty (default), the default logic of using the appName will be
   * used instead.
   */
  val akkaClusterBootstrapSystemName = SettingKey[String]("rp-akka-cluster-bootstrap-system-name")

  /**
   * Defines the endpoint name for the akka management port. reactive-lib expects the default values here so you
   * should not have to change this under normal circumstances.
   */
  val akkaManagementEndpointName = SettingKey[String]("rp-akka-management-endpoint-name")

  lazy val rpPackagingDockerCommmands = settingKey[Seq[CmdLike]]("Docker commands related to packaing.")

  lazy val rpApplicationConfig = taskKey[Config]("Used internally for configuration detetion")

  /**
   * Defines packages to install on top of the base alpine image.
   */
  @deprecated("""Use rpPackagingDockerCommmands:
                |import com.typesafe.sbt.packager.docker._
                |rpPackagingDockerCommmands := Vector(
                |  Cmd("RUN", "/sbin/apk", "add", "--no-cache", "bash", "coreutils"))""".stripMargin, "1.4.1")
  val alpinePackages = SettingKey[Seq[String]]("rp-alpine-packages")

  /**
   * Defines the available applications. This is a list of appName -> appArgs. The operator can specify alternate
   * applications to dynamically override the command/arguments that are executed.
   */
  val applications = TaskKey[Seq[(String, Seq[String])]]("rp-applications")

  /**
   * This task deploys all aggregated projects into a target environment. Currently, minikube is supported.
   */
  val deploy = InputKey[Unit]("deploy")

  /**
   * A map of service names to service lookup addresses. This will be provided as an argument to rp for resources
   * are are generated when running deploy minikube. Note that this map will only be added if reactive sandbox is
   * enabled.
   */
  val deployMinikubeReactiveSandboxExternalServices = SettingKey[Map[String, String]]("rp-deploy-minikube-reactive-sandbox-external-services")

  /**
   * An additional map of service names to service lookup addresses. These will always be provided to rp and take
   * precedence over the Reactive Sandbox addresses.
   */
  val deployMinikubeAdditionalExternalServices = SettingKey[Map[String, String]]("rp-deploy-minikube-additional-external-services")

  /**
   * When deploying applications with Akka Cluster Bootstrap enabled, the services will initially be started with this
   * many contact points / replicas.
   */
  val deployMinikubeAkkaClusterBootstrapContactPoints = SettingKey[Int]("rp-deploy-minikube-akka-cluster-bootstrap-contact-points")

  /**
   * If enabled, Reactive Sandbox (a Docker image containing Cassandra, Kafka, ZooKeeper, Elasticsearch) will be
   * deployed with this app.
   */
  val deployMinikubeEnableReactiveSandbox = SettingKey[Boolean]("rp-deploy-minikube-enable-reactive-sandbox")

  /**
   * If deploying a Play application, this property will be set to the Minikube IP.
   */
  val deployMinikubePlayHostAllowedProperty = SettingKey[String]("rp-deploy-minikube-play-host-allowed-property")

  /**
   * If deploying a Play application, this property will be set to the value specified below.
   */
  val deployMinikubePlayHttpSecretKeyProperty = SettingKey[String]("rp-deploy-minikube-play-http-secret-key-property")

  /**
   * If deploying a Play application, this property will be set to the value specified above.
   */
  val deployMinikubePlayHttpSecretKeyValue = SettingKey[String]("rp-deploy-minikube-play-http-secret-key-value")

  /**
   * Set this setting (build-wide, i.e. deployMinikubeReactiveSandboxCqlStatements in ThisBuild := ...) to a sequence
   * of CQL statements that should be executed against Cassandra when the Reactive Sandbox is installed.
   */
  val deployMinikubeReactiveSandboxCqlStatements = SettingKey[Seq[String]]("rp-deploy-minikube-reactive-sandbox-cql-statements")

  /**
   * Additional arguments to invoke rp with for this app.
   */
  val deployMinikubeRpArguments = SettingKey[Seq[String]]("rp-deploy-minikube-rp-arguments")

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
   * Enables Akka Cluster Bootstrapping (reactive-lib).
   */
  val enableAkkaClusterBootstrap = TaskKey[Boolean]("rp-enable-akka-cluster-bootstrap", "Include Akka Cluster Bootstrapping. By default, included if a Lagom persistence module is defined.")

  /**
   * Enables Akka Management (reactive-lib).
   *
   * By default, this is enabled if one of the following modules is enabled:
   *   - akka-cluster-bootstrap
   *   - status
   */
  val enableAkkaManagement = TaskKey[Boolean]("rp-enable-akka-management")

  /**
   * Enables the common library (reactive-lib). This defaults to true. It provides a few APIs that the application
   * is using to determine what target platform its running on, what ports it should bind on, etc.
   */
  val enableCommon = TaskKey[Boolean]("rp-enable-common")

  /**
   * Enable the Play HTTP binding library (reactive-lib). This is enabled by default for Lagom and Play projects and
   * allows the tooling to automatically assign ports.
   */
  val enablePlayHttpBinding = TaskKey[Boolean]("rp-enable-play-http-binding")

  /**
   * Enable the secret library (reactive-lib). By default, it will automatically be enabled if any secrets are
   * declared.
   */
  val enableSecrets = TaskKey[Boolean]("rp-enable-secrets", "Include Secrets API. By default, included if any secrets are defined.")

  /**
   * Enables the service discovery library (reactive-lib). If enabled, a service locator API will be on the classpath
   * and for Lagom projects, an implementation of Lagom's service locator will be provided.
   */
  val enableServiceDiscovery = TaskKey[Boolean]("rp-enable-service-discovery")

  /**
   * Enables the status library (reactive-lib). By default, it will automatically be enabled if any modules
   * that define health/readiness checks are enabled. Currently, this is only `akka-cluster-bootstrap`. At runtime,
   * routes for health and readiness will be added to the Akka Management HTTP server, and at resource generation
   * the appropriate health/readiness configuration will be generated to monitor these endpoints.
   */
  val enableStatus = TaskKey[Boolean]("rp-enable-status")

  /**
   * Executes `helm` program with any specified arguments. This may be expanded to support auto completion in
   * the future.
   */
  val helm = InputKey[Unit]("helm")

  /**
   * Executes `kubectl` program with any specified arguments. This may be expanded to support auto completion in
   * the future.
   */
  val kubectl = InputKey[Unit]("kubectl")

  /**
   * Executes `minikube` program with any specified arguments. This may be expanded to support auto completion in
   * the future.
   */
  val minikube = InputKey[Unit]("minikube")

  /**
   * If non-empty (default: "application.conf"), all unmanaged resources with the given name, in any dependencies,
   * as well as all managed resources named "rp-tooling.conf'",
   * will be concatenated into a managed resource "rp-application.conf" file. To disable this behavior, specify "".
   */
  val prependRpConf = SettingKey[String]("rp-prepend-rp-application-conf")

  /**
   * Defines the published reactive-lib version to include in the project. You can set this value to upgrade
   * reactive-lib without having to update sbt-reactive-app.
   */
  val reactiveLibVersion = SettingKey[String]("rp-reactive-lib-version")

  /**
   * Defines location where the wrapper script for app execution should be placed (in the container). If empty,
   * no wrapper script is used.
   */
  val startScriptLocation = SettingKey[String]("rp-start-script")

  /**
   * Defines secrets that will be made available to the application at runtime. The secrets API in reactive-lib
   * can then be used to decode these secrets in a consistent and platform-independent manner.
   */
  val secrets = SettingKey[Set[Secret]]("rp-secrets")

  /**
   * Annotations to attach to the application at runtime. On Kubernetes, this will become a pod annotation. On
   * DC/OS it will be a label.
   */
  val annotations = TaskKey[Map[String, String]](
    "rp-annotations",
    "Annotations to attach to the application at runtime. On Kubernetes, this will become a pod annotation. " +
      "On DC/OS it will be a label.")

  private[sbtreactiveapp] val deployMinikubeDockerEnv = TaskKey[String]("rp-deploy-minikube-docker-env")

  private[sbtreactiveapp] val lagomRawEndpoints = TaskKey[Seq[Endpoint]]("rp-lagom-raw-endpoints")

  private[sbtreactiveapp] val reactiveLibAkkaClusterBootstrapProject = SettingKey[(String, Boolean)]("rp-reactive-lib-akka-cluster-bootstrap-project")

  private[sbtreactiveapp] val reactiveLibCommonProject = SettingKey[(String, Boolean)]("rp-reactive-lib-common-project")

  private[sbtreactiveapp] val reactiveLibPlayHttpBindingProject = SettingKey[(String, Boolean)]("rp-reactive-lib-play-http-binding-project")

  private[sbtreactiveapp] val reactiveLibSecretsProject = SettingKey[(String, Boolean)]("rp-reactive-lib-secrets-project")

  private[sbtreactiveapp] val reactiveLibServiceDiscoveryProject = SettingKey[(String, Boolean)]("rp-reactive-lib-service-discovery-project")

  private[sbtreactiveapp] val reactiveLibStatusProject = SettingKey[(String, Boolean)]("rp-reactive-lib-status-project")
}

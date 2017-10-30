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
  val diskSpace = SettingKey[Option[Long]]("rp-disk-space")

  val memory = SettingKey[Option[Long]]("rp-memory")

  val nrOfCpus = SettingKey[Option[Double]]("rp-nr-of-cpus")

  val endpoints = TaskKey[Map[String, Endpoint]]("rp-endpoints")

  val volumes = TaskKey[Map[String, Volume]]("rp-volumes")

  val privileged = SettingKey[Boolean]("rp-privileged")

  val healthCheck = TaskKey[Option[Check]]("rp-health-check")

  val readinessCheck = TaskKey[Option[Check]]("rp-readiness-check")

  val environmentVariables = SettingKey[Map[String, EnvironmentVariable]]("rp-environment-variables")

  val reactiveLibProject = SettingKey[Option[String]]("rp-reactive-lib-project")

  val reactiveLibVersion = SettingKey[Option[String]]("rp-reactive-lib-version")

  val startScriptLocation = SettingKey[Option[String]]("rp-start-script")

  val secrets = SettingKey[Set[Secret]]("rp-secrets")
}

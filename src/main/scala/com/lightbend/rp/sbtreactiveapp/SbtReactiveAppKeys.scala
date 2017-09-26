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
  sealed trait Acl

  case class HttpAcl(expression: String) extends Acl

  case class TcpAcl(ports: Seq[Int]) extends Acl

  object TcpAcl {
    def apply(ports: Int*): TcpAcl = new TcpAcl(ports.toVector)
  }

  case class UdpAcl(ports: Seq[Int]) extends Acl

  object UdpAcl {
    def apply(ports: Int*): UdpAcl = new UdpAcl(ports.toVector)
  }

  case class Endpoint(protocol: String, port: Int, acls: Seq[Acl])

  object Endpoint {
    def apply(protocol: String, port: Int, acls: Acl*): Endpoint = new Endpoint(protocol, port, acls.toVector)
  }

  sealed trait Volume

  case class HostPathVolume(path: String) extends Volume

  case class SecretVolume(secret: String) extends Volume

  sealed trait Check

  case class CommandCheck(command: Seq[String]) extends Check

  object CommandCheck {
    def apply(command: String*): CommandCheck = new CommandCheck(command.toVector)
  }

  case class HttpCheck(port: Int, serviceName: String, intervalSeconds: Int) extends Check

  object HttpCheck {
    def apply(port: Int, intervalSeconds: Int): HttpCheck = HttpCheck(port, "", intervalSeconds)
    def apply(serviceName: String, intervalSeconds: Int): HttpCheck = HttpCheck(0, serviceName, intervalSeconds)
  }

  case class TcpCheck(port: Int, serviceName: String, intervalSeconds: Int) extends Check

  object TcpCheck {
    def apply(port: Int, intervalSeconds: Int): HttpCheck = HttpCheck(port, "", intervalSeconds)
    def apply(serviceName: String, intervalSeconds: Int): HttpCheck = HttpCheck(0, serviceName, intervalSeconds)
  }

  sealed trait EnvironmentVariable

  case class LiteralEnvironmentVariable(value: String) extends EnvironmentVariable

  case class SecretEnvironmentVariable(secret: String) extends EnvironmentVariable

  case class ConfigMapEnvironmentVariable(name: String, key: String) extends EnvironmentVariable

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
}

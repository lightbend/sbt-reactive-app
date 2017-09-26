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

package com.lightbend.sbtreactiveapp

import play.api.libs.json._
import SbtReactiveAppPlugin.autoImport._

object SbtReactiveApp {
  def labels(diskSpace: Option[Long],
             memory: Option[Long],
             nrOfCpus: Option[Double],
             endpoints: Map[String, Endpoint],
             volumes: Map[String, Volume],
             privileged: Boolean,
             healthCheck: Option[Check],
             readinessCheck: Option[Check],
             environmentVariables: Map[String, EnvironmentVariable]): Map[String, String] = {
    def ns(key: String) = s"com.lightbend.rp.$key"

    val keyValuePairs =
      diskSpace
        .map(ns("disk-space") -> _.toString)
        .toSeq ++
      memory
        .map(ns("memory") -> _.toString)
        .toSeq ++
      nrOfCpus
        .map(ns("nr-of-cpus") -> _.toString)
        .toSeq ++
      volumes
        .zipWithIndex
        .toSeq
        .flatMap { case ((guestPath, vol), i) =>
            vol match {
              case HostPathVolume(path) =>
                Vector(
                  ns(s"volumes.$i.type") -> "host-path",
                  ns(s"volumes.$i.path") -> path,
                  ns(s"volumes.$i.guest-path") -> guestPath
                )
              case SecretVolume(secret) =>
                Vector(
                  ns(s"volumes.$i.type") -> "secret",
                  ns(s"volumes.$i.secret") -> secret,
                  ns(s"volumes.$i.guest-path") -> guestPath
                )
            }
        } ++
      Vector(ns("privileged") -> privileged.toString) ++
      healthCheck
        .toSeq
        .flatMap(encodeCheck(suffix => ns(s"health-check.$suffix"))(_)) ++
      readinessCheck
        .toSeq
        .flatMap(encodeCheck(suffix => ns(s"readiness-check.$suffix"))(_)) ++
      environmentVariables
        .zipWithIndex
        .toSeq
        .flatMap { case ((envName, env), i) =>
          env match {
            case LiteralEnvironmentVariable(envValue) =>
              Vector(
                ns(s"environment-variables.$i.type") -> "literal",
                ns(s"environment-variables.$i.name") -> envName,
                ns(s"environment-variables.$i.value") -> envValue
              )
            case SecretEnvironmentVariable(secret) =>
              Vector(
                ns(s"environment-variables.$i.type") -> "secret",
                ns(s"environment-variables.$i.name") -> envName,
                ns(s"environment-variables.$i.secret") -> secret
              )
            case ConfigMapEnvironmentVariable(name, key) =>
              Vector(
                ns(s"environment-variables.$i.type") -> "configMap",
                ns(s"environment-variables.$i.name") -> name,
                ns(s"environment-variables.$i.key") -> key
              )
          }
        }

    keyValuePairs.toMap
  }

  private def encodeCheck(makeNs: String => String)(c: Check) = c match {
    case CommandCheck(args) =>
      Vector(
        makeNs("type") -> "command",
        makeNs("command") -> Json.toJson(args).toString
      )

    case HttpCheck(port, serviceName, intervalSeconds) =>
      if (port != 0)
        Vector(
          makeNs("type") -> "http",
          makeNs("port") -> port.toString,
          makeNs("interval") -> intervalSeconds.toString
        )
      else if (serviceName != "")
        Vector(
          makeNs("type") -> "http",
          makeNs("service-name") -> serviceName,
          makeNs("interval") -> intervalSeconds.toString
        )
      else
        Vector.empty

    case TcpCheck(port, serviceName, intervalSeconds) =>
      if (port != 0)
        Vector(
          makeNs("type") -> "http",
          makeNs("port") -> port.toString,
          makeNs("interval") -> intervalSeconds.toString
        )
      else if (serviceName != "")
        Vector(
          makeNs("type") -> "http",
          makeNs("service-name") -> serviceName,
          makeNs("interval") -> intervalSeconds.toString
        )
      else
        Vector.empty
  }
}

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

import SbtReactiveAppPlugin.autoImport._
import scala.collection.immutable.Seq

object SbtReactiveApp {
  def labels(appName: Option[String],
             diskSpace: Option[Long],
             memory: Option[Long],
             nrOfCpus: Option[Double],
             endpoints: Map[String, Endpoint],
             volumes: Map[String, Volume],
             privileged: Boolean,
             healthCheck: Option[Check],
             readinessCheck: Option[Check],
             environmentVariables: Map[String, EnvironmentVariable],
             version: Option[(Int, Int, Int, Option[String])]): Map[String, String] = {
    def ns(key: String*): String = (Seq("com", "lightbend", "rp") ++ key).mkString(".")

    val keyValuePairs =
      appName
        .map(ns("app-name") -> _.toString)
        .toSeq ++
      diskSpace
        .map(ns("disk-space") -> _.toString)
        .toSeq ++
      memory
        .map(ns("memory") -> _.toString)
        .toSeq ++
      nrOfCpus
        .map(ns("nr-of-cpus") -> _.toString)
        .toSeq ++
      endpoints
        .toSeq
        .zipWithIndex
        .flatMap { case ((name, endpoint), i) =>
          val baseKeys = Vector(
            ns("endpoints", i.toString, "name") -> name,
            ns("endpoints", i.toString, "protocol") -> endpoint.protocol
          )

          val portKey =
            if (endpoint.port != 0)
              Vector(ns(s"endpoints", i.toString, "port") -> endpoint.port.toString)
            else
              Vector.empty

          val aclKeys = endpoint
            .acls
            .zipWithIndex
            .flatMap { case (acl, j) =>
              acl match {
                case HttpAcl(expression) =>
                  Vector(
                    ns("endpoints", i.toString, "acls", j.toString, "type") -> "http",
                    ns("endpoints", i.toString, "acls", j.toString, "expression") -> expression
                  )
              }
            }

          baseKeys ++ portKey ++ aclKeys
        } ++
      volumes
        .toSeq
        .zipWithIndex
        .flatMap { case ((guestPath, vol), i) =>
            vol match {
              case HostPathVolume(path) =>
                Vector(
                  ns("volumes", i.toString, "type") -> "host-path",
                  ns("volumes", i.toString, "path") -> path,
                  ns("volumes", i.toString, "guest-path") -> guestPath
                )
              case SecretVolume(secret) =>
                Vector(
                  ns("volumes", i.toString, "type") -> "secret",
                  ns("volumes", i.toString, "secret") -> secret,
                  ns("volumes", i.toString, "guest-path") -> guestPath
                )
            }
        } ++
      (if (privileged) Some(ns("privileged") -> "true") else None).toVector ++
      healthCheck
        .toSeq
        .flatMap(/*_*/ encodeCheck(suffix => ns("health-check" +: suffix: _*)) /*_*/) ++
      readinessCheck
        .toSeq
        .flatMap(/*_*/ encodeCheck(suffix => ns("readiness-check" +: suffix: _*)) /*_*/) ++
      environmentVariables
        .toSeq
        .zipWithIndex
        .flatMap { case ((envName, env), i) =>
          env match {
            case LiteralEnvironmentVariable(envValue) =>
              Vector(
                ns(s"environment-variables", i.toString, "type") -> "literal",
                ns(s"environment-variables", i.toString, "name") -> envName,
                ns(s"environment-variables", i.toString, "value") -> envValue
              )
            case SecretEnvironmentVariable(secret) =>
              Vector(
                ns(s"environment-variables", i.toString, "type") -> "secret",
                ns(s"environment-variables", i.toString, "name") -> envName,
                ns(s"environment-variables", i.toString, "secret") -> secret
              )
            case kubernetes.ConfigMapEnvironmentVariable(mapName, key) =>
              Vector(
                ns(s"environment-variables", i.toString, "type") -> "configMap",
                ns(s"environment-variables", i.toString, "name") -> envName,
                ns(s"environment-variables", i.toString, "map-name") -> mapName,
                ns(s"environment-variables", i.toString, "key") -> key
              )
          }
        } ++
      version
        .toSeq
        .flatMap { case (major, minor, patch, maybeLabel) =>
          Vector(
            ns("version-major") -> major.toString,
            ns("version-minor") -> minor.toString,
            ns("version-patch") -> patch.toString) ++
          maybeLabel.toVector.map(label => ns("version-patch-label") -> label)
        }

    keyValuePairs.toMap
  }

  private def encodeCheck(makeNs: (String*) => String)(c: Check) = c match {
    case CommandCheck(args) =>
      Vector(makeNs("type") -> "command") ++
      args
        .zipWithIndex
        .map { case (arg, i) =>
          makeNs("args", i.toString) -> arg
        }

    case HttpCheck(port, serviceName, intervalSeconds, path) =>
      if (port != 0)
        Vector(
          makeNs("type") -> "http",
          makeNs("port") -> port.toString,
          makeNs("interval") -> intervalSeconds.toString,
          makeNs("path") -> path
        )
      else if (serviceName != "")
        Vector(
          makeNs("type") -> "http",
          makeNs("service-name") -> serviceName,
          makeNs("interval") -> intervalSeconds.toString,
          makeNs("path") -> path
        )
      else
        Vector.empty

    case TcpCheck(port, serviceName, intervalSeconds) =>
      if (port != 0)
        Vector(
          makeNs("type") -> "tcp",
          makeNs("port") -> port.toString,
          makeNs("interval") -> intervalSeconds.toString
        )
      else if (serviceName != "")
        Vector(
          makeNs("type") -> "tcp",
          makeNs("service-name") -> serviceName,
          makeNs("interval") -> intervalSeconds.toString
        )
      else
        Vector.empty
  }
}

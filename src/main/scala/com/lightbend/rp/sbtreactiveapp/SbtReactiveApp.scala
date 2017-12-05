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
  def labels(
    namespace: Option[String],
    appName: Option[String],
    appType: Option[String],
    configResource: Option[String],
    diskSpace: Option[Long],
    memory: Option[Long],
    nrOfCpus: Option[Double],
    endpoints: Seq[Endpoint],
    volumes: Map[String, Volume],
    privileged: Boolean,
    healthCheck: Option[Check],
    readinessCheck: Option[Check],
    environmentVariables: Map[String, EnvironmentVariable],
    version: Option[String],
    secrets: Set[Secret],
    modules: Seq[(String, Boolean)]): Map[String, String] = {
    def ns(key: String*): String = (Seq("com", "lightbend", "rp") ++ key).mkString(".")

    val keyValuePairs =
      namespace
        .map(ns("namespace") -> _.toString)
        .toSeq ++
        appName
        .map(ns("app-name") -> _.toString)
        .toSeq ++
        version
        .map(ns("app-version") -> _)
        .toSeq ++
        appType
        .map(ns("app-type") -> _)
        .toSeq ++
        configResource
        .map(ns("config-resource") -> _)
        .toSeq ++
        modules
        .map { case (m, enabled) => ns("modules", m, "enabled") -> enabled.toString } ++
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
        .zipWithIndex
        .flatMap {
          case (endpoint, i) =>
            val baseKeys = Vector(
              ns("endpoints", i.toString, "name") -> endpoint.name,
              ns("endpoints", i.toString, "protocol") -> endpoint.protocol)

            val portKey =
              if (endpoint.port != 0)
                Vector(ns(s"endpoints", i.toString, "port") -> endpoint.port.toString)
              else
                Vector.empty

            def encodeHttpIngress(h: HttpIngress, j: Int) =
              Vector(ns("endpoints", i.toString, "ingress", j.toString, "type") -> "http") ++
                h.ingressPorts.zipWithIndex.map {
                  case (port, k) =>
                    ns("endpoints", i.toString, "ingress", j.toString, "ingress-ports", k.toString) -> port.toString
                } ++
                h.hosts.zipWithIndex.map {
                  case (host, k) =>
                    ns("endpoints", i.toString, "ingress", j.toString, "hosts", k.toString) -> host
                } ++
                h.paths.toVector.zipWithIndex.map {
                  case (path, k) =>
                    ns("endpoints", i.toString, "ingress", j.toString, "paths", k.toString) -> path
                }

            def encodePortIngress(p: PortIngress) =
              Vector(ns("endpoints", i.toString, "ingress", "type") -> "port") ++
                p.ingressPorts.zipWithIndex.map {
                  case (port, j) =>
                    ns("endpoints", i.toString, "ingress", "ingress-ports", j.toString) -> port.toString
                }

            val ingressKeys =
              endpoint match {
                case HttpEndpoint(_, _, ingress) =>
                  ingress.zipWithIndex.flatMap { case (h, j) => encodeHttpIngress(h, j) }
                case TcpEndpoint(_, _, ingress) =>
                  ingress.toVector.flatMap(encodePortIngress)
                case UdpEndpoint(_, _, ingress) =>
                  ingress.toVector.flatMap(encodePortIngress)
              }

            baseKeys ++ portKey ++ ingressKeys
        } ++
        volumes
        .toSeq
        .zipWithIndex
        .flatMap {
          case ((guestPath, vol), i) =>
            vol match {
              case HostPathVolume(path) =>
                Vector(
                  ns("volumes", i.toString, "type") -> "host-path",
                  ns("volumes", i.toString, "path") -> path,
                  ns("volumes", i.toString, "guest-path") -> guestPath)
            }
        } ++
        (if (privileged) Some(ns("privileged") -> "true") else None).toVector ++
        healthCheck
        .toSeq
        .flatMap( /*_*/ encodeCheck(suffix => ns("health-check" +: suffix: _*)) /*_*/ ) ++
        readinessCheck
        .toSeq
        .flatMap( /*_*/ encodeCheck(suffix => ns("readiness-check" +: suffix: _*)) /*_*/ ) ++
        environmentVariables
        .toSeq
        .zipWithIndex
        .flatMap {
          case ((envName, env), i) =>
            env match {
              case LiteralEnvironmentVariable(envValue) =>
                Vector(
                  ns("environment-variables", i.toString, "type") -> "literal",
                  ns("environment-variables", i.toString, "name") -> envName,
                  ns("environment-variables", i.toString, "value") -> envValue)
              case kubernetes.ConfigMapEnvironmentVariable(mapName, key) =>
                Vector(
                  ns("environment-variables", i.toString, "type") -> "kubernetes.configMap",
                  ns("environment-variables", i.toString, "name") -> envName,
                  ns("environment-variables", i.toString, "map-name") -> mapName,
                  ns("environment-variables", i.toString, "key") -> key)
              case kubernetes.FieldRefEnvironmentVariable(fieldPath) =>
                Vector(
                  ns("environment-variables", i.toString, "type") -> "kubernetes.fieldRef",
                  ns("environment-variables", i.toString, "name") -> envName,
                  ns("environment-variables", i.toString, "field-path") -> fieldPath)
            }
        } ++
        secrets
        .toSeq
        .zipWithIndex
        .flatMap {
          case (secret, i) =>
            Vector(
              ns("secrets", i.toString, "namespace") -> secret.namespace,
              ns("secrets", i.toString, "name") -> secret.name)
        }

    keyValuePairs.toMap
  }

  private def encodeCheck(makeNs: (String*) => String)(c: Check) = c match {
    case CommandCheck(args) =>
      Vector(makeNs("type") -> "command") ++
        args
        .zipWithIndex
        .map {
          case (arg, i) =>
            makeNs("args", i.toString) -> arg
        }

    case HttpCheck(port, serviceName, intervalSeconds, path) =>
      if (port != 0)
        Vector(
          makeNs("type") -> "http",
          makeNs("port") -> port.toString,
          makeNs("interval") -> intervalSeconds.toString,
          makeNs("path") -> path)
      else if (serviceName != "")
        Vector(
          makeNs("type") -> "http",
          makeNs("service-name") -> serviceName,
          makeNs("interval") -> intervalSeconds.toString,
          makeNs("path") -> path)
      else
        Vector.empty

    case TcpCheck(port, serviceName, intervalSeconds) =>
      if (port != 0)
        Vector(
          makeNs("type") -> "tcp",
          makeNs("port") -> port.toString,
          makeNs("interval") -> intervalSeconds.toString)
      else if (serviceName != "")
        Vector(
          makeNs("type") -> "tcp",
          makeNs("service-name") -> serviceName,
          makeNs("interval") -> intervalSeconds.toString)
      else
        Vector.empty
  }
}

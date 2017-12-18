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

package com.lightbend.rp.sbtreactiveapp.magic

import com.lightbend.rp.sbtreactiveapp._
import play.api.libs.json.{ JsObject, Json }
import sbt._

import scala.collection.immutable.Seq
import scala.language.reflectiveCalls
import scala.util.Try

object Lagom {
  def component(id: String): Option[ModuleID] = {
    // The method signature equals the signature of `com.lightbend.lagom.sbt.LagomImport`
    type LagomImport = {
      def component(id: String): ModuleID
    }

    withContextClassloader(this.getClass.getClassLoader) { loader =>
      getSingletonObject[LagomImport](loader, "com.lightbend.lagom.sbt.LagomImport$")
        .map(_.component(id))
        .toOption
    }
  }

  def endpoints(classPath: Seq[Attributed[File]], scalaLoader: ClassLoader, ports: Seq[Int], hosts: Seq[String], paths: Seq[String]): Option[Seq[Endpoint]] =
    services(classPath, scalaLoader).map(decodeServices(_, ports, hosts, paths))

  def hasCluster(libraryDependencies: Seq[ModuleID]): Boolean = {
    // we can't inspect all transitive dependencies because the entire class path can't be calculated until
    // we decide whether we need to add the cluster or not, so we resort to this hack which covers
    // out of the box cases. If a custom module is used that uses persistence features, the user
    // would have to explicitly enable the SBT setting instead of relying on autodetection

    libraryDependencies.exists(d =>
      d.organization == "com.lightbend.lagom" && (
        d.name.contains("-persistence") || d.name.contains("-pubsub") || d.name.contains("-cluster")))
  }

  def lagomJavaPlugin(classLoader: ClassLoader): Try[AutoPlugin] =
    withContextClassloader(classLoader) { loader =>
      getSingletonObject[AutoPlugin](loader, "com.lightbend.lagom.sbt.LagomJava$")
    }

  def lagomScalaPlugin(classLoader: ClassLoader): Try[AutoPlugin] =
    withContextClassloader(classLoader) { loader =>
      getSingletonObject[AutoPlugin](loader, "com.lightbend.lagom.sbt.LagomScala$")
    }

  def lagomPlayJavaPlugin(classLoader: ClassLoader): Try[AutoPlugin] =
    withContextClassloader(classLoader) { loader =>
      getSingletonObject[AutoPlugin](loader, "com.lightbend.lagom.sbt.LagomPlayJava$")
    }

  def lagomPlayScalaPlugin(classLoader: ClassLoader): Try[AutoPlugin] =
    withContextClassloader(classLoader) { loader =>
      getSingletonObject[AutoPlugin](loader, "com.lightbend.lagom.sbt.LagomPlayScala$")
    }

  def services(classPath: Seq[Attributed[File]], scalaLoader: ClassLoader): Option[String] = {
    // `ServiceDetector` mirror from the Lagom api tools library.
    // The method signature equals the signature from the api tools `ServiceDetector`
    type ServiceDetector = {
      def services(classLoader: ClassLoader): String
    }

    val classLoader = new java.net.URLClassLoader(classPath.files.map(_.toURI.toURL).toArray, scalaLoader)

    withContextClassloader(classLoader) { loader =>
      getSingletonObject[ServiceDetector](loader, "com.lightbend.lagom.internal.api.tools.ServiceDetector$")
        .map(_.services(loader))
        .toOption
    }
  }

  def version: Option[String] = {
    // The method signature equals the signature of `com.lightbend.lagom.core.LagomVersion`
    type LagomVersion = {
      def current: String
    }

    withContextClassloader(this.getClass.getClassLoader) { loader =>
      getSingletonObject[LagomVersion](loader, "com.lightbend.lagom.core.LagomVersion$")
        .map(_.current)
        .toOption
    }
  }

  private def decodeServices(services: String, ports: Seq[Int], hosts: Seq[String], paths: Seq[String]): Seq[HttpEndpoint] = {
    def toEndpoint(serviceName: String, pathBegins: Seq[String]): HttpEndpoint = {
      // If we're provided an explicit path listing, use that instead. A future improvement would be to
      // move path autodetection to a separate task

      val pathsToUse =
        if (paths.nonEmpty)
          paths
        else
          pathBegins.distinct.filter(_.nonEmpty)

      if (pathsToUse.nonEmpty)
        HttpEndpoint(serviceName, HttpIngress(ports, hosts, pathsToUse))
      else
        HttpEndpoint(serviceName)
    }

    def mergeEndpoint(endpoints: Seq[HttpEndpoint], endpointEntry: HttpEndpoint): Seq[HttpEndpoint] = {
      val mergedEndpoint =
        endpoints
          .find(_.name == endpointEntry.name)
          .fold(endpointEntry) { prevEndpoint =>
            prevEndpoint.copy(ingress = prevEndpoint.ingress ++ endpointEntry.ingress)
          }

      endpoints.filterNot(_.name == endpointEntry.name) :+ mergedEndpoint
    }

    Json
      .parse(services)
      .as[Seq[JsObject]].map { o =>
        val serviceName = (o \ "name").as[String]
        val pathlessServiceName = if (serviceName.startsWith("/")) serviceName.drop(1) else serviceName
        val pathBegins = (o \ "acls" \\ "pathPattern")
          .map(_.as[String])
          .toVector
          .collect {
            case pathBeginExtractor(pathBegin) =>
              if (pathBegin.endsWith("/"))
                pathBegin.dropRight(1)
              else
                pathBegin
          }

        toEndpoint(pathlessServiceName, pathBegins)
      }
      .foldLeft(Seq.empty[HttpEndpoint])(mergeEndpoint)
  }

  // Matches strings that starts with sequence escaping, e.g. \Q/api/users/:id\E
  // The first sequence escaped substring that starts with a '/' is extracted as a variable
  // Examples:
  // /api/users                         => false
  // \Q/\E                              => true, variable = /
  // \Q/api/users\E                     => true, variable = /api/users
  // \Q/api/users/\E([^/]+)             => true, variable = /api/users/
  // \Q/api/users/\E([^/]+)\Q/friends\E => true, variable = /api/users/
  private val pathBeginExtractor = """^\\Q(\/.*?)\\E.*""".r
}

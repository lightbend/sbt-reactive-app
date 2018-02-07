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

package com.lightbend.rp.sbtreactiveapp.cmd

import sbt.Logger
import scala.collection.immutable.Seq

object minikube {
  def assert(): Unit = {
    val minMajor = 0
    val minMinor = 25

    val (code, out, _) = run()("minikube", "version")

    if (code != 0) {
      sys.error(s"minikube is not installed [$code]")
    } else {
      val version = out.headOption.getOrElse("")

      parseVersion(version) match {
        case Some((major, minor, _)) if major > minMajor || (major == minMajor && minor >= minMinor) =>
          val minikubeRunning = run()("minikube", "status")._1 == 0

          if (!minikubeRunning) {
            sys.error(s"minikube is not running; start it with `minikube start`; enabled ingress with `minikube addons enable ingress`")
          }
        case None =>
          sys.error(s"minikube version unparseable [$version]")
        case Some(_) =>
          sys.error(s"minikube version too old, required $minMajor.$minMinor.0 or newer [$version]")
      }
    }
  }

  def invoke(logger: Logger, args: Seq[String]): Unit =
    runSuccess("minikube failed")(run(logStdErr = Some(logger), logStdOut = Some(logger))("minikube" +: args: _*))

  def ip(): String = {
    val (code, out, _) = run()("minikube", "ip")

    if (code != 0 || out.isEmpty) {
      sys.error(s"minikube ip failed [$code]")
    }

    out.head.trim
  }

  private[cmd] def parseVersion(version: String): Option[(Int, Int, Int)] =
    "v([0-9]+)[.]([0-9]+)[.]([0-9]+).*"
      .r
      .findFirstMatchIn(version)
      .map { matches =>
        (matches.group(1).toInt, matches.group(2).toInt, matches.group(3).toInt)
      }
}

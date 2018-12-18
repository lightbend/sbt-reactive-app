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

import play.api.libs.json._
import sbt.Logger
import scala.collection.immutable.Seq

object kubectl {
  private val WaitSleepTimeMs = 1000L
  private val WaitStatusEvery = 10

  def assert(): Unit =
    runSuccess("kubectl is not installed")(run()("kubectl", "version"))

  def setupHelmRBAC(logger: Logger, systemNamespace: String): Unit = {
    val (c1, _, err) = run()("kubectl", "-n", systemNamespace, "create", "sa", "tiller")
    if (c1 != 0 && !err.exists(_.contains("AlreadyExists"))) {
      sys.error(s"failed to create tiller service account [$c1]")
    }

    val (c2, _, _) = run()("kubectl", "create", "clusterrolebinding",
      "tiller", "--clusterrole", "cluster-admin", s"--serviceaccount=$systemNamespace:tiller")
    if (c2 != 0) {
      sys.error(s"failed to create tiller cluster role binding [$c2]")
    }
  }

  def deleteAndApply(logger: Logger, yaml: String): Unit = {
    // app may not be deployed yet, in which case this fails and sends output to stderr. note that because of this
    // we're ignoring stderr.

    run(input = Some(yaml), logStdOut = Some(logger))("kubectl", "delete", "-f", "-", "--grace-period=0")

    val (code, _, _) =
      run(input = Some(yaml), logStdErr = Some(logger), logStdOut = Some(logger))("kubectl", "apply", "-f", "-")

    if (code != 0) {
      sys.error(s"rp apply failed [$code]")
    }
  }

  def deploymentExists(namespace: String, name: String): Boolean =
    try {
      run()("kubectl", "get", "--namespace", namespace, s"deploy/$name")._1 == 0
    } catch {
      case _: Throwable => false
    }

  def deploymentReady(namespace: String, name: String): Boolean =
    try {
      val (code, out, _) =
        run()("kubectl", "get", "--namespace", namespace, s"deploy/$name", "-o", "json")

      if (code != 0) {
        sys.error(s"kubectl get deploy/$name failed [$code]")
      }

      val parsed = Json.parse(out.mkString("\n"))

      val replicas = (parsed \ "spec" \ "replicas").as[JsNumber]

      val readyReplicas = (parsed \ "status" \ "readyReplicas").as[JsNumber]

      readyReplicas.value == replicas.value
    } catch {
      case _: Throwable => false
    }

  def getPodNames(selector: String): Seq[String] = {
    val (code, stdout, _) = run()("kubectl", "get", "pods", "-o", "name", s"--selector=$selector")

    if (code != 0) {
      sys.error(s"kubectl get pods failed [$code]")
    }

    stdout.flatMap { line =>
      val parts = line.split("/", 2)

      if (parts.length == 2)
        Some(parts(1))
      else
        None
    }
  }

  def invoke(logger: Logger, args: Seq[String]): Unit =
    runSuccess("kubectl failed")(run(logStdErr = Some(logger), logStdOut = Some(logger))("kubectl" +: args: _*))

  /**
   * Waits for a deployment to be ready -- that is, the specified desired replicas
   * equals the number of readyReplicas. Doesn't necessary have to be deployment, but
   * the metadata paths for it are hardcoded for now. Support for other pod controllers
   * can be added at a later date.
   */
  def waitForDeployment(logger: Logger, namespace: String, name: String, waitTimeMs: Int): Unit = {
    val startTime = System.currentTimeMillis()
    val maxTime = startTime + waitTimeMs

    @annotation.tailrec
    def wait(n: Int): Unit = {
      if (!deploymentReady(namespace, name)) {
        if (n == 0) {
          logger.info(s"Waiting for deploy/$name to become ready...")
        }

        Thread.sleep(WaitSleepTimeMs)

        if (System.currentTimeMillis() >= maxTime) {
          sys.error(s"deploy/$name is still not ready")
        }

        wait(if (n == WaitStatusEvery) 0 else n + 1)
      }
    }

    wait(0)
  }
}

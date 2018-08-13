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

object helm {
  def assert(): Unit =
    runSuccess("helm is not installed")(run()("helm", "--help"))

  def init(logger: Logger, serviceAccount: String): Unit = {
    runSuccess("helm init failed")(
      run(logStdErr = Some(logger), logStdOut = Some(logger))("helm", "init", "--service-account", serviceAccount))

    runSuccess("helm repo add failed")(
      run(logStdErr = Some(logger), logStdOut = Some(logger))("helm", "repo", "add", "lightbend-helm-charts", "https://lightbend.github.io/helm-charts"))

    runSuccess("helm repo update")(
      run(logStdErr = Some(logger), logStdOut = Some(logger))("helm", "repo", "update"))
  }

  def installReactiveSandbox(logger: Logger): Unit =
    runSuccess("helm failed to install reactive sandbox")(
      run(logStdErr = Some(logger), logStdOut = Some(logger))("helm", "install", "lightbend-helm-charts/reactive-sandbox", "--name", "reactive-sandbox"))

  def invoke(logger: Logger, args: Seq[String]): Unit =
    runSuccess("helm failed")(run(logStdErr = Some(logger), logStdOut = Some(logger))("helm" +: args: _*))
}

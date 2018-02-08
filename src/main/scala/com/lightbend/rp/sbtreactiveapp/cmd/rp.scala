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

import com.lightbend.rp.sbtreactiveapp._;
import sbt.Logger
import scala.collection.immutable.Seq

object rp {
  def assert(): Unit = runSuccess("reactive-cli is not installed")(run()("rp", "version"))

  def generateKubernetesResources(wrapper: String, logger: Logger, additionalArgs: Seq[String]): String = {
    val rpCommand =
      (if (isWindows) Vector("powershell.exe", wrapper, "rp.exe") else Vector(wrapper, "rp")) ++
        Vector("generate-kubernetes-resources", "--generate-all") ++
        additionalArgs

    val (code, stdout, _) = run(logStdErr = Some(logger))(rpCommand: _*)

    if (code != 0) {
      sys.error(s"rp generate-kubernetes-resources failed [$code]")
    }

    stdout.mkString("\n")
  }
}

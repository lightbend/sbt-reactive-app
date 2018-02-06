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

import java.io.File
import java.nio.file.Paths
import org.apache.tools.ant.filters.StringInputStream
import sbt.Logger
import scala.collection.immutable.Seq
import scala.sys.process.{ Process, ProcessLogger }

package object cmd {
  /**
   * Runs a process and returns (statusCode, stdOut, stdErr)
   */
  private[cmd] def run(
    cwd: File = Paths.get(".").toRealPath().toFile,
    env: Map[String, String] = Map.empty,
    input: Option[String] = None,
    logStdErr: Option[Logger] = None,
    logStdOut: Option[Logger] = None)(args: String*): (Int, Seq[String], Seq[String]) = {
    var outList = List.empty[String]
    var errList = List.empty[String]

    val stringLogger = ProcessLogger(
      { s =>
        outList = s :: outList

        logStdOut.foreach(_.info(s))
      },
      { s =>
        errList = s :: errList

        logStdErr.foreach(_.error(s))
      })

    val exitCode =
      input
        .map(new StringInputStream(_))
        .foldLeft(Process(args, cwd = cwd, env.toVector: _*))(_ #< _)
        .run(stringLogger)
        .exitValue()

    (exitCode, outList.reverse, errList.reverse)
  }

  private[cmd] def runSuccess(failMsg: String)(result: (Int, Seq[String], Seq[String])): Unit = {
    if (result._1 != 0) {
      sys.error(s"$failMsg [${result._1}]")
    }
  }
}

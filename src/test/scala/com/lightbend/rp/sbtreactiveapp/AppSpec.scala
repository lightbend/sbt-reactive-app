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

import com.typesafe.sbt.packager.docker
import scala.collection.immutable.Seq

class AppSpec extends UnitSpec {
  "normalizeName" should {
    Seq(
      "hello" -> "hello",
      "hello there" -> "hello-there",
      "?? hello?there++=))" -> "hello-there").foreach { x =>
        val (input, expectedResult) = x
        s"normalize [$input] to [$expectedResult]" in {
          App.normalizeName(input) shouldBe expectedResult
        }
      }
  }

  "labelCommand" should {
    "work for empty" in {
      BasicApp.labelCommand(Seq.empty) shouldBe Seq.empty
    }

    "work for single" in {
      BasicApp.labelCommand(Seq("a" -> "test")) shouldBe Seq(docker.Cmd("LABEL", "a=\"test\""))
    }

    "work for multiple" in {
      BasicApp.labelCommand(
        Seq("a" -> "test", "b" -> "test2", "c" -> "test3")) shouldBe Seq(docker.Cmd("LABEL", "a=\"test\" \\\nb=\"test2\" \\\nc=\"test3\""))
    }
  }
}

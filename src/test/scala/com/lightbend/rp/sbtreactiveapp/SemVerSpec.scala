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

class SemVerSpec extends UnitSpec {
  "parse" should {
    "correctly parse a semantic version number" in {
      SemVer.parse("1.2.3") shouldEqual Some((1, 2, 3, None))
      SemVer.parse("1.2.3-SNAPSHOT") shouldEqual Some((1, 2, 3, Some("SNAPSHOT")))
    }

    "fail to parse a non-semantic version number" in {
      SemVer.parse("abc.xyz.123") shouldEqual None
    }
  }
}

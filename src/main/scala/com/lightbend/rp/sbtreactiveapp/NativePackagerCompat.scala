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

import com.typesafe.sbt.packager.docker.DockerAlias

/**
 * `sbt-native-packager` is not SemVer compliant and classes like `DockerAlias` break binary compatibility
 * in patch releases. This object provides makes all 1.3.x versions of `sbt-native-packager` compatible.
 */
object NativePackagerCompat {

  // removed from `sbt-native-packager` (https://github.com/sbt/sbt-native-packager/pull/1138/files#diff-6aa67d5df515952c884df8bad5ff2ac4L12)
  def untagged(dockerAlias: DockerAlias): String =
    dockerAlias.registryHost.map(_ + "/").getOrElse("") +
      dockerAlias.username.map(_ + "/").getOrElse("") + dockerAlias.name

  // removed from `sbt-native-packager` (https://github.com/sbt/sbt-native-packager/pull/1138/files#diff-6aa67d5df515952c884df8bad5ff2ac4L12)
  def versioned(dockerAlias: DockerAlias): String = {
    untagged(dockerAlias) + dockerAlias.tag.map(":" + _).getOrElse("")
  }

  // removed from `sbt-native-packager` (https://github.com/sbt/sbt-native-packager/pull/1138/files#diff-6aa67d5df515952c884df8bad5ff2ac4L12)
  def latest(dockerAlias: DockerAlias) = s"${untagged(dockerAlias)}:latest"
}

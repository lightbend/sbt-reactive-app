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

import scala.language.reflectiveCalls

object Lagom {
  def isJava: Boolean = localObjectExists("com.lightbend.lagom.sbt.LagomJava$")

  def isPlayJava: Boolean = localObjectExists("com.lightbend.lagom.sbt.LagomPlayJava$")

  def isPlayScala: Boolean = localObjectExists("com.lightbend.lagom.sbt.LagomPlayScala$")

  def isScala: Boolean = localObjectExists("com.lightbend.lagom.sbt.LagomScala$")

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

  private def localObjectExists(className: String): Boolean =
    withContextClassloader(this.getClass.getClassLoader) { loader =>
      objectExists(loader, className)
    }
}

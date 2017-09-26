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

import java.lang.reflect.InvocationTargetException
import scala.reflect.ClassTag
import scala.util.Try

package object magic {
  def getSingletonObject[T: ClassTag](classLoader: ClassLoader, className: String): Try[T] =
    Try {
      val clazz = classLoader.loadClass(className)
      val t = implicitly[ClassTag[T]].runtimeClass
      clazz.getField("MODULE$").get(null) match {
        case null                  => throw new NullPointerException
        case c if !t.isInstance(c) => throw new ClassCastException(s"${clazz.getName} is not a subtype of $t")
        case c: T                  => c
      }
    } recover {
      case i: InvocationTargetException if i.getTargetException ne null => throw i.getTargetException
    }

  def objectExists(classLoader: ClassLoader, className: String): Boolean =
    try {
      classLoader.loadClass(className).getField("MODULE$").get(null) != null
    } catch {
      case _: Exception => false
    }

  def withContextClassloader[T](loader: ClassLoader)(body: ClassLoader => T): T = {
    val current = Thread.currentThread().getContextClassLoader
    try {
      Thread.currentThread().setContextClassLoader(loader)
      body(loader)
    } finally Thread.currentThread().setContextClassLoader(current)
  }
}

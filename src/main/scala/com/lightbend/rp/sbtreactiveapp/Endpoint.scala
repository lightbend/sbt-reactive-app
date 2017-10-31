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

import scala.collection.immutable.Seq

sealed trait Endpoint {
  def name: String
  def port: Int
  def protocol: String
  def version: Option[Version]
}

case class HttpEndpoint(name: String, port: Int, ingress: Seq[Ingress], version: Option[Version] = Some(MajorVersion)) extends Endpoint {
  val protocol: String = "http"
}

object HttpEndpoint {
  def apply(name: String, port: Int, ingress: Ingress*): HttpEndpoint = new HttpEndpoint(name, port, ingress.toVector)
}

case class TcpEndpoint(name: String, port: Int, ingress: Seq[PortIngress], version: Option[Version] = Some(MajorVersion)) extends Endpoint {
  val protocol: String = "tcp"
}

object TcpEndpoint {
  def apply(name: String, port: Int, ingress: PortIngress*): TcpEndpoint = new TcpEndpoint(name, port, ingress.toVector)
}

case class UdpEndpoint(name: String, port: Int, ingress: Seq[PortIngress], version: Option[Version] = Some(MajorVersion)) extends Endpoint {
  val protocol: String = "udp"
}

object UdpEndpoint {
  def apply(name: String, port: Int, ingress: PortIngress*): UdpEndpoint = new UdpEndpoint(name, port, ingress.toVector)
}

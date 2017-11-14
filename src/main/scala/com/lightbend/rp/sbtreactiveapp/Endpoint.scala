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

case class HttpEndpoint(name: String, port: Int, ingress: Seq[HttpIngress], version: Option[Version] = Some(MajorVersion)) extends Endpoint {
  val protocol: String = "http"
}

object HttpEndpoint {
  def apply(name: String): HttpEndpoint = new HttpEndpoint(name, 0, Vector.empty)
  def apply(name: String, ingress: HttpIngress*): HttpEndpoint = new HttpEndpoint(name, 0, ingress.toVector)
  def apply(name: String, port: Int): HttpEndpoint = new HttpEndpoint(name, port, Vector.empty)
  def apply(name: String, port: Int, ingress: HttpIngress*): HttpEndpoint = new HttpEndpoint(name, port, ingress.toVector)
}

case class TcpEndpoint(name: String, port: Int, ingress: Option[PortIngress], version: Option[Version] = Some(MajorVersion)) extends Endpoint {
  val protocol: String = "tcp"
}

object TcpEndpoint {
  def apply(name: String): TcpEndpoint = new TcpEndpoint(name, 0, None)
  def apply(name: String, ingress: PortIngress): TcpEndpoint = new TcpEndpoint(name, 0, Some(ingress))
  def apply(name: String, port: Int): TcpEndpoint = new TcpEndpoint(name, port, None)
  def apply(name: String, port: Int, ingress: PortIngress): TcpEndpoint = new TcpEndpoint(name, port, Some(ingress))
}

case class UdpEndpoint(name: String, port: Int, ingress: Option[PortIngress], version: Option[Version] = Some(MajorVersion)) extends Endpoint {
  val protocol: String = "udp"
}

object UdpEndpoint {
  def apply(name: String): UdpEndpoint = new UdpEndpoint(name, 0, None)
  def apply(name: String, ingress: PortIngress): UdpEndpoint = new UdpEndpoint(name, 0, Some(ingress))
  def apply(name: String, port: Int): UdpEndpoint = new UdpEndpoint(name, port, None)
  def apply(name: String, port: Int, ingress: PortIngress): UdpEndpoint = new UdpEndpoint(name, port, Some(ingress))
}

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

class SbtReactiveAppSpec extends UnitSpec {
  "labels" should {
    "work for defaults" in {
      SbtReactiveApp.labels(
        appName = None,
        appType = None,
        configResource = None,
        diskSpace = None,
        memory = None,
        cpu = None,
        endpoints = Vector.empty,
        volumes = Map.empty,
        privileged = false,
        environmentVariables = Map.empty,
        version = None,
        secrets = Set.empty,
        modules = Vector.empty,
        akkaClusterBootstrapSystemName = None) shouldBe Map.empty
    }

    "work for all values (except checks)" in {
      SbtReactiveApp.labels(
        appName = Some("myapp"),
        appType = Some("mytype"),
        configResource = Some("my-config.conf"),
        diskSpace = Some(1234),
        memory = Some(5678),
        cpu = Some(0.25),
        endpoints = Vector(
          HttpEndpoint("ep1", 1234),
          HttpEndpoint(
            "ep2",
            5678,
            HttpIngress(ingressPorts = Vector(80, 443), hosts = Vector("hello.world.com", "goodbye.cruel.world.com"), paths = Vector("/frontend/my-url.*$", "/frontend/my-other-url.*$")),
            HttpIngress(ingressPorts = Vector(80, 443), hosts = Vector("secret.hello.world.com", "secret.goodbye.cruel.world.com"), paths = Vector("/backend/my-url.*$", "/backend/my-other-url.*$"))),
          TcpEndpoint("ep3", 9123, PortIngress(8080)),
          UdpEndpoint("ep4", 8123, PortIngress(8080)),
          HttpEndpoint("ep5", 1235, ingress = Vector.empty),
          HttpEndpoint("ep6", 1236, ingress = Vector.empty),
          HttpEndpoint("ep7", 1237, ingress = Vector.empty)),
        volumes = Map(
          "/data/vol1" -> HostPathVolume("/var/lib/vol1"),
          "/data/vol2" -> HostPathVolume("/var/lib/vol2")),
        privileged = true,
        environmentVariables = Map(
          "env1" -> LiteralEnvironmentVariable("my env one"),
          "env2" -> kubernetes.ConfigMapEnvironmentVariable("my-map", "my-key"),
          "env3" -> kubernetes.FieldRefEnvironmentVariable("my-field-path")),
        version = Some("1.2.3-SNAPSHOT"),
        secrets = Set(Secret("myns1", "myname1"), Secret("myns2", "myname2")),
        modules = Vector("mod1" -> true, "mod2" -> false),
        akkaClusterBootstrapSystemName = Some("test")) shouldBe Map(

          "com.lightbend.rp.app-name" -> "myapp",
          "com.lightbend.rp.app-type" -> "mytype",
          "com.lightbend.rp.config-resource" -> "my-config.conf",
          "com.lightbend.rp.modules.mod1.enabled" -> "true",
          "com.lightbend.rp.modules.mod2.enabled" -> "false",
          "com.lightbend.rp.disk-space" -> "1234",
          "com.lightbend.rp.memory" -> "5678",
          "com.lightbend.rp.cpu" -> "0.25",
          "com.lightbend.rp.privileged" -> "true",
          "com.lightbend.rp.endpoints.0.name" -> "ep1",
          "com.lightbend.rp.endpoints.0.port" -> "1234",
          "com.lightbend.rp.endpoints.0.protocol" -> "http",
          "com.lightbend.rp.endpoints.1.name" -> "ep2",
          "com.lightbend.rp.endpoints.1.port" -> "5678",
          "com.lightbend.rp.endpoints.1.protocol" -> "http",
          "com.lightbend.rp.endpoints.1.ingress.0.type" -> "http",
          "com.lightbend.rp.endpoints.1.ingress.0.paths.0" -> "/frontend/my-url.*$",
          "com.lightbend.rp.endpoints.1.ingress.0.paths.1" -> "/frontend/my-other-url.*$",
          "com.lightbend.rp.endpoints.1.ingress.0.hosts.0" -> "hello.world.com",
          "com.lightbend.rp.endpoints.1.ingress.0.hosts.1" -> "goodbye.cruel.world.com",
          "com.lightbend.rp.endpoints.1.ingress.0.ingress-ports.0" -> "80",
          "com.lightbend.rp.endpoints.1.ingress.0.ingress-ports.1" -> "443",
          "com.lightbend.rp.endpoints.1.ingress.1.type" -> "http",
          "com.lightbend.rp.endpoints.1.ingress.1.paths.0" -> "/backend/my-url.*$",
          "com.lightbend.rp.endpoints.1.ingress.1.paths.1" -> "/backend/my-other-url.*$",
          "com.lightbend.rp.endpoints.1.ingress.1.hosts.0" -> "secret.hello.world.com",
          "com.lightbend.rp.endpoints.1.ingress.1.hosts.1" -> "secret.goodbye.cruel.world.com",
          "com.lightbend.rp.endpoints.1.ingress.1.ingress-ports.0" -> "80",
          "com.lightbend.rp.endpoints.1.ingress.1.ingress-ports.1" -> "443",
          "com.lightbend.rp.endpoints.2.name" -> "ep3",
          "com.lightbend.rp.endpoints.2.protocol" -> "tcp",
          "com.lightbend.rp.endpoints.2.port" -> "9123",
          "com.lightbend.rp.endpoints.2.ingress.type" -> "port",
          "com.lightbend.rp.endpoints.2.ingress.ingress-ports.0" -> "8080",
          "com.lightbend.rp.endpoints.3.name" -> "ep4",
          "com.lightbend.rp.endpoints.3.protocol" -> "udp",
          "com.lightbend.rp.endpoints.3.port" -> "8123",
          "com.lightbend.rp.endpoints.3.ingress.type" -> "port",
          "com.lightbend.rp.endpoints.3.ingress.ingress-ports.0" -> "8080",
          "com.lightbend.rp.endpoints.4.name" -> "ep5",
          "com.lightbend.rp.endpoints.4.port" -> "1235",
          "com.lightbend.rp.endpoints.4.protocol" -> "http",
          "com.lightbend.rp.endpoints.5.name" -> "ep6",
          "com.lightbend.rp.endpoints.5.port" -> "1236",
          "com.lightbend.rp.endpoints.5.protocol" -> "http",
          "com.lightbend.rp.endpoints.6.name" -> "ep7",
          "com.lightbend.rp.endpoints.6.port" -> "1237",
          "com.lightbend.rp.endpoints.6.protocol" -> "http",
          "com.lightbend.rp.volumes.0.type" -> "host-path",
          "com.lightbend.rp.volumes.0.path" -> "/var/lib/vol1",
          "com.lightbend.rp.volumes.0.guest-path" -> "/data/vol1",
          "com.lightbend.rp.volumes.1.type" -> "host-path",
          "com.lightbend.rp.volumes.1.path" -> "/var/lib/vol2",
          "com.lightbend.rp.volumes.1.guest-path" -> "/data/vol2",
          "com.lightbend.rp.environment-variables.0.name" -> "env1",
          "com.lightbend.rp.environment-variables.0.type" -> "literal",
          "com.lightbend.rp.environment-variables.0.value" -> "my env one",
          "com.lightbend.rp.environment-variables.1.name" -> "env2",
          "com.lightbend.rp.environment-variables.1.type" -> "kubernetes.configMap",
          "com.lightbend.rp.environment-variables.1.map-name" -> "my-map",
          "com.lightbend.rp.environment-variables.1.key" -> "my-key",
          "com.lightbend.rp.environment-variables.2.name" -> "env3",
          "com.lightbend.rp.environment-variables.2.type" -> "kubernetes.fieldRef",
          "com.lightbend.rp.environment-variables.2.field-path" -> "my-field-path",
          "com.lightbend.rp.app-version" -> "1.2.3-SNAPSHOT",
          "com.lightbend.rp.secrets.0.name" -> "myns1",
          "com.lightbend.rp.secrets.0.key" -> "myname1",
          "com.lightbend.rp.secrets.1.name" -> "myns2",
          "com.lightbend.rp.secrets.1.key" -> "myname2",
          "com.lightbend.rp.akka-cluster-bootstrap.system-name" -> "test")
    }
  }
}

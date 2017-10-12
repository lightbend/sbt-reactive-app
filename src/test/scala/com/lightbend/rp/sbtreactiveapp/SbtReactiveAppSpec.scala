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
        diskSpace = None,
        memory = None,
        nrOfCpus = None,
        endpoints = Map.empty,
        volumes = Map.empty,
        privileged = false,
        healthCheck = None,
        readinessCheck = None,
        environmentVariables = Map.empty,
        version = None) shouldBe Map.empty
    }

    "work for all values (except checks)" in {
      SbtReactiveApp.labels(
        diskSpace = Some(1234),
        memory = Some(5678),
        nrOfCpus = Some(0.25),
        endpoints = Map(
          "ep1" -> Endpoint("http", 1234),
          "ep2" -> Endpoint("http", 5678, HttpAcl("^/my-url.*$"), HttpAcl("^/my-other-url.*$")),
          "ep3" -> Endpoint("tcp", 9123, TcpAcl(9123, 9124), TcpAcl(9125)),
          "ep4" -> Endpoint("udp", 8123, UdpAcl(8123, 8124), UdpAcl(8125))
        ),
        volumes = Map(
          "/data/vol1" -> HostPathVolume("/var/lib/vol1"),
          "/data/vol2" -> SecretVolume("my-secret")
        ),
        privileged = true,
        healthCheck = None,
        readinessCheck = None,
        environmentVariables = Map(
          "env1" -> LiteralEnvironmentVariable("my env one"),
          "env2" -> SecretEnvironmentVariable("my-secret"),
          "env3" -> kubernetes.ConfigMapEnvironmentVariable("my-map", "my-key")),
        version = Some((1, 2, 3, Some("SNAPSHOT")))) shouldBe Map(

        "com.lightbend.rp.disk-space" -> "1234",
        "com.lightbend.rp.memory" -> "5678",
        "com.lightbend.rp.nr-of-cpus" -> "0.25",
        "com.lightbend.rp.privileged" -> "true",
        "com.lightbend.rp.endpoints.0.name" -> "ep1",
        "com.lightbend.rp.endpoints.0.port" -> "1234",
        "com.lightbend.rp.endpoints.0.protocol" -> "http",
        "com.lightbend.rp.endpoints.1.name" -> "ep2",
        "com.lightbend.rp.endpoints.1.port" -> "5678",
        "com.lightbend.rp.endpoints.1.protocol" -> "http",
        "com.lightbend.rp.endpoints.1.acls.0.type" -> "http",
        "com.lightbend.rp.endpoints.1.acls.0.expression" -> "^/my-url.*$",
        "com.lightbend.rp.endpoints.1.acls.1.type" -> "http",
        "com.lightbend.rp.endpoints.1.acls.1.expression" -> "^/my-other-url.*$",
        "com.lightbend.rp.endpoints.2.name" -> "ep3",
        "com.lightbend.rp.endpoints.2.protocol" -> "tcp",
        "com.lightbend.rp.endpoints.2.port" -> "9123",
        "com.lightbend.rp.endpoints.2.acls.0.type" -> "tcp",
        "com.lightbend.rp.endpoints.2.acls.0.ports.0" -> "9123",
        "com.lightbend.rp.endpoints.2.acls.0.ports.1" -> "9124",
        "com.lightbend.rp.endpoints.2.acls.1.type" -> "tcp",
        "com.lightbend.rp.endpoints.2.acls.1.ports.0" -> "9125",
        "com.lightbend.rp.endpoints.3.name" -> "ep4",
        "com.lightbend.rp.endpoints.3.protocol" -> "udp",
        "com.lightbend.rp.endpoints.3.port" -> "8123",
        "com.lightbend.rp.endpoints.3.acls.0.type" -> "udp",
        "com.lightbend.rp.endpoints.3.acls.0.ports.0" -> "8123",
        "com.lightbend.rp.endpoints.3.acls.0.ports.1" -> "8124",
        "com.lightbend.rp.endpoints.3.acls.1.type" -> "udp",
        "com.lightbend.rp.endpoints.3.acls.1.ports.0" -> "8125",
        "com.lightbend.rp.volumes.0.type" -> "host-path",
        "com.lightbend.rp.volumes.0.path" -> "/var/lib/vol1",
        "com.lightbend.rp.volumes.0.guest-path" -> "/data/vol1",
        "com.lightbend.rp.volumes.1.type" -> "secret",
        "com.lightbend.rp.volumes.1.secret" -> "my-secret",
        "com.lightbend.rp.volumes.1.guest-path" -> "/data/vol2",
        "com.lightbend.rp.environment-variables.0.name" -> "env1",
        "com.lightbend.rp.environment-variables.0.type" -> "literal",
        "com.lightbend.rp.environment-variables.0.value" -> "my env one",
        "com.lightbend.rp.environment-variables.1.name" -> "env2",
        "com.lightbend.rp.environment-variables.1.type" -> "secret",
        "com.lightbend.rp.environment-variables.1.secret" -> "my-secret",
        "com.lightbend.rp.environment-variables.2.name" -> "env3",
        "com.lightbend.rp.environment-variables.2.type" -> "configMap",
        "com.lightbend.rp.environment-variables.2.map-name" -> "my-map",
        "com.lightbend.rp.environment-variables.2.key" -> "my-key",
        "com.lightbend.rp.version-major" -> "1",
        "com.lightbend.rp.version-minor" -> "2",
        "com.lightbend.rp.version-patch" -> "3",
        "com.lightbend.rp.version-patch-label" -> "SNAPSHOT")
    }

    "work for tcp checks" in {
      SbtReactiveApp.labels(
        diskSpace = None,
        memory = None,
        nrOfCpus = None,
        endpoints = Map.empty,
        volumes = Map.empty,
        privileged = false,
        healthCheck = Some(TcpCheck(80, 10)),
        readinessCheck = Some(TcpCheck(90, 5)),
        environmentVariables = Map.empty,
        version = None) shouldBe Map(
          "com.lightbend.rp.health-check.type" -> "tcp",
          "com.lightbend.rp.health-check.port" -> "80",
          "com.lightbend.rp.health-check.interval" -> "10",
          "com.lightbend.rp.readiness-check.type" -> "tcp",
          "com.lightbend.rp.readiness-check.port" -> "90",
          "com.lightbend.rp.readiness-check.interval" -> "5")

      SbtReactiveApp.labels(
        diskSpace = None,
        memory = None,
        nrOfCpus = None,
        endpoints = Map.empty,
        volumes = Map.empty,
        privileged = false,
        healthCheck = Some(TcpCheck("test", 10)),
        readinessCheck = Some(TcpCheck("test2", 5)),
        environmentVariables = Map.empty,
        version = None) shouldBe Map(
          "com.lightbend.rp.health-check.type" -> "tcp",
          "com.lightbend.rp.health-check.service-name" -> "test",
          "com.lightbend.rp.health-check.interval" -> "10",
          "com.lightbend.rp.readiness-check.type" -> "tcp",
          "com.lightbend.rp.readiness-check.service-name" -> "test2",
          "com.lightbend.rp.readiness-check.interval" -> "5")
    }

    "work for http checks" in {
      SbtReactiveApp.labels(
        diskSpace = None,
        memory = None,
        nrOfCpus = None,
        endpoints = Map.empty,
        volumes = Map.empty,
        privileged = false,
        healthCheck = Some(HttpCheck(80, 10, "/health")),
        readinessCheck = Some(HttpCheck(90, 5, "/other-health")),
        environmentVariables = Map.empty,
        version = None) shouldBe Map(
        "com.lightbend.rp.health-check.type" -> "http",
        "com.lightbend.rp.health-check.port" -> "80",
        "com.lightbend.rp.health-check.interval" -> "10",
        "com.lightbend.rp.health-check.path" -> "/health",
        "com.lightbend.rp.readiness-check.type" -> "http",
        "com.lightbend.rp.readiness-check.port" -> "90",
        "com.lightbend.rp.readiness-check.interval" -> "5",
        "com.lightbend.rp.readiness-check.path" -> "/other-health")

      SbtReactiveApp.labels(
        diskSpace = None,
        memory = None,
        nrOfCpus = None,
        endpoints = Map.empty,
        volumes = Map.empty,
        privileged = false,
        healthCheck = Some(HttpCheck("test", 10, "/health")),
        readinessCheck = Some(HttpCheck("test2", 5, "/other-health")),
        environmentVariables = Map.empty,
        version = None) shouldBe Map(
        "com.lightbend.rp.health-check.type" -> "http",
        "com.lightbend.rp.health-check.service-name" -> "test",
        "com.lightbend.rp.health-check.interval" -> "10",
        "com.lightbend.rp.health-check.path" -> "/health",
        "com.lightbend.rp.readiness-check.type" -> "http",
        "com.lightbend.rp.readiness-check.service-name" -> "test2",
        "com.lightbend.rp.readiness-check.interval" -> "5",
        "com.lightbend.rp.readiness-check.path" -> "/other-health")
    }

    "work for command checks" in {
      SbtReactiveApp.labels(
        diskSpace = None,
        memory = None,
        nrOfCpus = None,
        endpoints = Map.empty,
        volumes = Map.empty,
        privileged = false,
        healthCheck = Some(CommandCheck("/bin/bash", "arg one", "arg two")),
        readinessCheck = Some(CommandCheck("/bin/ash", "arg 1", "arg 2")),
        environmentVariables = Map.empty,
        version = None) shouldBe Map(
        "com.lightbend.rp.health-check.type" -> "command",
        "com.lightbend.rp.health-check.args.0" -> "/bin/bash",
        "com.lightbend.rp.health-check.args.1" -> "arg one",
        "com.lightbend.rp.health-check.args.2" -> "arg two",
        "com.lightbend.rp.readiness-check.type" -> "command",
        "com.lightbend.rp.readiness-check.args.0" -> "/bin/ash",
        "com.lightbend.rp.readiness-check.args.1" -> "arg 1",
        "com.lightbend.rp.readiness-check.args.2" -> "arg 2")
    }
  }
}

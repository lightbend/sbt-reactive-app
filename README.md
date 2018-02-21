# sbt-reactive-app

[![GitHub version](https://img.shields.io/badge/version-0.6.1-blue.svg)](https://github.com/lightbend/reactive-cli/releases)
[![Build Status](https://api.travis-ci.org/lightbend/sbt-reactive-app.png?branch=master)](https://travis-ci.org/lightbend/sbt-reactive-app)

This project is a component of [Lightbend Orchestration for Kubernetes](https://developer.lightbend.com/docs/lightbend-orchestration-kubernetes/latest/). Refer to its documentation for usage, examples, and reference information.

It implements an SBT plugin that uses [SBT Native Packager](https://github.com/sbt/sbt-native-packager) and
reflection to build Docker images. Images produced by this plugin can be used with a CLI tool, [reactive-cli](https://github.com/lightbend/reactive-cli), to create resources for Kubernetes and potentially other target platforms.

## Usage

Consult the [Lightbend Orchestration for Kubernetes](https://developer.lightbend.com/docs/lightbend-orchestration-kubernetes/latest/) documentation for setup and configuration.

## Development

##### Publish Locally

`sbt ^publishLocal`

## Release

Consult "Platform Tooling Release Process" on Google Drive

## Maintenance

Enterprise Suite Platform Team <es-platform@lightbend.com>

## License

Copyright (C) 2017 Lightbend Inc. (https://www.lightbend.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

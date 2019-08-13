# sbt-reactive-app

[![Build Status](https://api.travis-ci.org/lightbend/sbt-reactive-app.png?branch=master)](https://travis-ci.org/lightbend/sbt-reactive-app)

This project is a component of [Lightbend Orchestration](https://developer.lightbend.com/docs/lightbend-orchestration/current/). Refer to its documentation for usage, examples, and reference information.

It implements an SBT plugin that uses [sbt Native Packager](https://github.com/sbt/sbt-native-packager) and
reflection to build Docker images. Images produced by this plugin can be used with a CLI tool, [reactive-cli](https://github.com/lightbend/reactive-cli), to create resources for Kubernetes and potentially other target platforms.

## Project Status

Lightbend Orchestration is no longer actively developed and will reach its [End of Life](https://developer.lightbend.com/docs/lightbend-platform/introduction/getting-help/support-terminology.html#eol) on April 15, 2020.

We recommend [Migrating to the Improved Kubernetes Deployment Experience](https://developer.lightbend.com/docs/lightbend-orchestration/current/migration.html).

## Usage

Consult the [Lightbend Orchestration](https://developer.lightbend.com/docs/lightbend-orchestration/current/) documentation for setup and configuration.

## Development

##### Publish Locally

`sbt ^publishLocal`

## Release

Consult "Lightbend Orchestration Release Process" on Google Drive

## License

Copyright (C) 2017 Lightbend Inc. (https://www.lightbend.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

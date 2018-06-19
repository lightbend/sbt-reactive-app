name := "prepend-rp-conf"
scalaVersion := "2.12.4"
enablePlugins(SbtReactiveAppPlugin)

// Disable creation of rp-application.conf.
prependRpConf := ""

// Add a few dependencies that contain rp-tooling.conf.
libraryDependencies += "com.lightbend.rp" % "reactive-lib-status_2.12" % "0.7.0"
libraryDependencies += "com.lightbend.rp" % "reactive-lib-akka-management_2.12" % "0.7.0"
libraryDependencies += "com.lightbend.rp" % "reactive-lib-service-discovery_2.12" % "0.7.0"

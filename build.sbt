sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-slick"

libraryDependencies += "com.typesafe.slick" %% "slick-codegen" % "2.1.0"

scalacOptions += "-deprecation"

scriptedSettings

scriptedBufferLog := false

scriptedLaunchOpts <+= version(v => "-Dproject.version=" + v)
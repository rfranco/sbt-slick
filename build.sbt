sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-slick"

libraryDependencies += "com.typesafe.slick" %% "slick" % "2.1.0-RC2"

scalacOptions += "-deprecation"

scriptedSettings

scriptedLaunchOpts <+= version(v => "-Dproject.version=" + v)
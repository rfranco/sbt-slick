lazy val root = (project in file(".")).enablePlugins(SbtSlick)

libraryDependencies += "com.h2database" % "h2" % "1.3.170"

SlickKeys.url := "jdbc:h2:target/db/test;AUTO_SERVER=TRUE"
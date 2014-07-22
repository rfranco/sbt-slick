lazy val root = (project in file(".")).enablePlugins(SbtSlick)

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "2.1.0-RC2",
  "com.h2database" % "h2" % "1.3.170"
)

excludeFilter in Slick := "*.COMPANies"

SlickKeys.driver := "org.h2.Driver"

SlickKeys.url := "jdbc:h2:mem:test;INIT=runscript from 'src/main/sql/create.sql'"
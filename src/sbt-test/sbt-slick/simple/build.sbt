lazy val root = (project in file(".")).enablePlugins(SbtSlick)

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.h2database" % "h2" % "1.3.170"
)

excludeFilter in slick := "*.COMPANIES"

SlickKeys.jdbcDriver := "org.h2.Driver"

SlickKeys.url := "jdbc:h2:mem:test;INIT=runscript from 'src/main/sql/create.sql'"
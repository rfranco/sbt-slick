package com.typesafe.sbt.slick

import scala.slick.{model => m}
import slick.codegen.SourceCodeGenerator
import slick.driver._
import slick.jdbc.JdbcBackend
import sbt.Keys._
import sbt._
import sbt.classpath.ClasspathUtilities

object Import {

  val Slick = settingKey[Unit]("Slick Table")

  object SlickKeys {

    val url = settingKey[String]("Database URL")
    val user = settingKey[Option[String]]("Database username")
    val password = settingKey[Option[String]]("Database password")
    val driver = settingKey[String]("Database driver class name")

    val imports = settingKey[Seq[String]]("Imports")
    val pkg = settingKey[String]("Package")

    val codegen = settingKey[m.Model => SourceCodeGenerator]("Factory for code generator")
    private[slick] val slickCodeGen = taskKey[Unit]("Generate code")
  }

}

object SbtSlick extends AutoPlugin {

  val autoImport = Import

  import Import._

  override val projectSettings = Seq(
    SlickKeys.user := None,
    SlickKeys.password := None,

    SlickKeys.pkg := organization.value,
    SlickKeys.imports := Nil,

    SlickKeys.codegen := {
      new BasicSourceGen(_)
    },
    SlickKeys.slickCodeGen := generateCode.value,

    includeFilter in Slick := NothingFilter,
    excludeFilter in Slick := NothingFilter
  )

  private val driverByName = Map(
    "org.apache.derby.jdbc.EmbeddedDriver" -> DerbyDriver,
    "org.h2.Driver" -> H2Driver,
    "org.hsqldb.jdbcDriver" -> HsqldbDriver,
    "com.mysql.jdbc.Driver" -> MySQLDriver,
    "org.postgresql.Driver" -> PostgresDriver,
    "org.sqlite.JDBC" -> SQLiteDriver
  )

  private lazy val jdbcProfile = Def.task[JdbcProfile] {
    val driver = SlickKeys.driver.value
    driverByName.get(driver).get // give Error
  }

  private lazy val database = Def.task[JdbcBackend#Database] {
    val driver = SlickKeys.driver.value
    val profile = jdbcProfile.value

    val fc = fullClasspath.in(Runtime).value
    val classLoader = ClasspathUtilities.toLoader(fc.map(_.data))
    val driverObject = Class.forName(driver, true, classLoader).newInstance.asInstanceOf[java.sql.Driver]

    profile.simple.Database.forDriver(
      driver = driverObject,
      url = SlickKeys.url.value,
      user = SlickKeys.user.value.orNull,
      password = SlickKeys.password.value.orNull
    )
  }

  private lazy val generateModel = Def.task {
    val db = database.value
    val profile = jdbcProfile.value

    val include = includeFilter.in(Slick).value
    val exclude = excludeFilter.in(Slick).value

    db.withSession { implicit session =>
      val tables = profile.defaultTables

      tables.filter { t => import t.name._
        val fullName = catalog.map(_ + ".").getOrElse("") + schema.map(_ + ".").getOrElse("") + name
        val table = new File(fullName)
        include.accept(table) || !exclude.accept(table)
      }

      profile.createModel(Some(tables))
    }
  }

  private lazy val generateCode = Def.task {
    val model = generateModel.value
    val gen = SlickKeys.codegen.value(model)

    val folder = scalaSource.in(Compile).value
    val pkg = SlickKeys.pkg.value
    val name = "Tables"
    val imports = SlickKeys.imports.value
    val code = gen.indent(gen.code)

    val content = packageCode(code, pkg, name, imports)
    val file = folder / pkg.replace(".", "/") / (name + ".scala")

    IO.write(file, content)
  }

  private def packageCode(code: String, pkg: String, name: String, imports: Seq[String] = Nil): String = s"""
package $pkg
import scala.slick.driver.JdbcProfile
${imports.map(i => s"import $i").mkString("\n")}
/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait $name {
  val profile: JdbcProfile
  import profile.simple._
  $code
}"""

}

class BasicSourceGen(model: m.Model) extends SourceCodeGenerator(model) {

  override def Table = new Table(_) {
    override def autoIncLastAsOption: Boolean = super.autoIncLastAsOption
    override def hlistEnabled: Boolean = super.hlistEnabled
    override def mappingEnabled: Boolean = super.mappingEnabled

    override def EntityType = new EntityType {
    }

    override def PlainSqlMapper = new PlainSqlMapper {
    }

    override def TableClass = new TableClass {
    }

    override def TableValue = new TableValue {
    }

    override def Column = new Column(_) {
    }

    override def PrimaryKey = new PrimaryKey(_) {
    }

    override def ForeignKey = new ForeignKey(_) {
    }

    override def Index = new Index(_) {
    }

  }
}
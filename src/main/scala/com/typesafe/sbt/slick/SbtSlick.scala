package com.typesafe.sbt.slick

import scala.slick.codegen.SourceCodeGenerator
import scala.slick.driver._
import scala.slick.jdbc.JdbcBackend
import scala.slick.{model => m}
import sbt.Keys._
import sbt._
import sbt.classpath.ClasspathUtilities

object Import {

  type Model = m.Model

  val slick = settingKey[Unit]("Slick Table")

  object SlickKeys {

    val url = settingKey[String]("Database URL")
    val user = settingKey[Option[String]]("Database username")
    val password = settingKey[Option[String]]("Database password")
    val jdbcDriver = settingKey[String]("Database driver class name")

    val imports = settingKey[Seq[String]]("Imports")
    val pkg = settingKey[String]("Package")
    val sourceName = SettingKey[String]("Source Name")

    val sourceCodegen = settingKey[Model => SourceCodeGenerator]("Factory for code generator")
    val packageCodegen = settingKey[String => String]("Factory for package code generator")
    private[slick] val slickCodeGen = taskKey[Unit]("Generate code")
  }

}

object SbtSlick extends AutoPlugin {

  val autoImport = Import

  import com.typesafe.sbt.slick.Import._

  override val projectSettings = Seq(
    SlickKeys.user := None,
    SlickKeys.password := None,

    SlickKeys.pkg := organization.value,
    SlickKeys.imports := Nil,
    SlickKeys.sourceName := "Tables",

    SlickKeys.sourceCodegen := { model: Model =>
      new SourceCodeGenerator(model)
    },
    SlickKeys.packageCodegen := packageCodegen.value,
    SlickKeys.slickCodeGen := generateCode.value,

    includeFilter in slick := NothingFilter,
    excludeFilter in slick := NothingFilter
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
    val driver = SlickKeys.jdbcDriver.value
    driverByName.get(driver).get // give Error
  }

  private lazy val database = Def.task[JdbcBackend#Database] {
    val driver = SlickKeys.jdbcDriver.value
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

    val include = includeFilter.in(slick).value
    val exclude = excludeFilter.in(slick).value

    db.withSession { implicit session =>
      val tables = profile.defaultTables

      val filteredTables = tables.filter { t => import t.name._
        val fullName = schema.map(_ + ".").getOrElse("") + name
        val table = new File(fullName)
        println(fullName)
        println( include.accept(table))
        println(!exclude.accept(table))
        include.accept(table) && !exclude.accept(table)
      }

      profile.createModel(Some(filteredTables))
    }
  }

  private lazy val generateCode = Def.task {
    val model = generateModel.value
    val gen = SlickKeys.sourceCodegen.value(model)
    val code = gen.indent(gen.code)
    val content = SlickKeys.packageCodegen.value(code)
    val folder = scalaSource.in(Compile).value
    val pkg = SlickKeys.pkg.value
    val name = SlickKeys.sourceName.value
    val file = folder / pkg.replace(".", "/") / (name + ".scala")
    IO.write(file, content)
  }

  private lazy val packageCodegen = Def.setting { code: String =>
    val pkg = SlickKeys.pkg.value
    val name = SlickKeys.sourceName.value
    val imports = SlickKeys.imports.value
    s"""package $pkg
import scala.slick.driver.JdbcProfile
${imports.map(i => s"import $i").mkString("\n")}
/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait $name {
  val profile: JdbcProfile
  import profile.simple._
  $code
}"""
  }

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
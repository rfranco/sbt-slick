package com.typesafe.sbt.slick

import sbt._

object Import {

  object SlickKeys {

    val url = settingKey[String]("Database URL")
    val user = settingKey[Option[String]]("Database username")
    val password = settingKey[Option[String]]("Database password")
    val driver = settingKey[Option[String]]("Database driver class name")

  }

}

object SbtSlick extends AutoPlugin {

  val autoImport = Import

  import Import._

  override val projectSettings = Seq(
    SlickKeys.user := None,
    SlickKeys.password := None,
    SlickKeys.driver := None
  )

}

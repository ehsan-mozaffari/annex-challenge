ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.annex"

val zioVersion        = "2.0.19"
val tapirVersion      = "1.9.6"
val zioJsonVersion    = "0.6.2"
val zioConfigVersion  = "4.0.0-RC16"
val zioLoggingVersion = "2.1.14"
val zioHttpVersion    = "3.0.0-RC2"

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Ykind-projector",
    "-source:future",
    "-Xmax-inlines",
    "64"
  )
)

lazy val root = (project in file("."))
  .settings(
    name          := "annex-challenge",
    Compile / run := (core / Compile / run).evaluated
  )
  .aggregate(core, domain, api)

lazy val domain = (project in file("modules/domain"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "dev.zio"                     %% "zio"        % zioVersion,
      "dev.zio"                     %% "zio-json"   % zioJsonVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-core"              % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-zio"          % tapirVersion    )
  )

lazy val core = (project in file("modules/core"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"                 % zioVersion,
      "dev.zio" %% "zio-streams"         % zioVersion,
      "dev.zio" %% "zio-logging"         % zioLoggingVersion,
      "dev.zio" %% "zio-config"          % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
    )
  )
  .dependsOn(domain, api)

lazy val api = (project in file("modules/api"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core"              % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio"               % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"   % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-zio"          % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "dev.zio"                     %% "zio-http"                % zioHttpVersion,
      "dev.zio"                     %% "zio-http-testkit"        % zioHttpVersion % Test
    )
  )
  .dependsOn(domain)

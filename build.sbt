import scalariform.formatter.preferences._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val scala212 = "2.12.13"
val scala213 = "2.13.6"
val scala3 = "3.0.0"

val scalatestVersion = "3.2.9"
val scalacheckVersion = "1.15.4"

ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq(scala212, scala213, scala3)

lazy val commonSettings = Seq(
  organization := "org.gnieh",
  version := "4.1.1",
  description := "Json diff/patch library",
  licenses += ("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/gnieh/diffson")),
  parallelExecution := false,
  scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
    case Some((2, n)) if n >= 13 =>
      Seq(
        "-Ymacro-annotations",
        "-Ytasty-reader"
      )
    case Some((3, _)) =>
      Seq(
        "-Ykind-projector"
      )
  }.toList.flatten,
  libraryDependencies ++=
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq(
          compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
          compilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full)
        )
      case Some((2, 13))=>
        Seq(
          compilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full)
        )
      case _ =>
        Nil
    }),
  scalariformAutoformat := true,
  scalariformPreferences := {
    scalariformPreferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
  },
  coverageExcludedPackages := "<empty>;.*Test.*",
  Compile / doc / scalacOptions ++= Seq("-groups", "-implicits"),
  autoAPIMappings := true,
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")) ++ Seq(
    scalariformPreferences := {
    scalariformPreferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
      .setPreference(DanglingCloseParenthesis, Prevent)
    }) ++ publishSettings

lazy val diffson = project.in(file("."))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(crossScalaVersions := Nil)
  .settings(
    name := "diffson",
    packagedArtifacts := Map()
  )
  .aggregate(core.jvm, core.js, sprayJson, circe.jvm, circe.js, playJson.jvm, playJson.js, testkit.jvm, testkit.js)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure).in(file("core"))
  .enablePlugins(ScoverageSbtPlugin, ScalaUnidocPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-core",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.4.4",
      "org.typelevel"  %%% "cats-core"  % "2.6.1",
      "org.scalatest"  %%% "scalatest"  % scalatestVersion % Test,
      "org.scalacheck" %%% "scalacheck" % scalacheckVersion % Test
    ))
  .jsSettings(coverageEnabled := false)

lazy val testkit = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full).in(file("testkit"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-testkit",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalatestVersion,
      "org.scalacheck" %%% "scalacheck" % scalacheckVersion))
  .jsSettings(coverageEnabled := false)
  .dependsOn(core)

lazy val sprayJson = project.in(file("sprayJson"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-spray-json",
    crossScalaVersions := Seq(scala212, scala213),
    libraryDependencies += "io.spray" %%  "spray-json" % "1.3.6")
  .dependsOn(core.jvm, testkit.jvm % Test)

lazy val playJson = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full).in(file("playJson"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-play-json",
    libraryDependencies += "com.typesafe.play" %%% "play-json" % "2.9.2",
    crossScalaVersions := Seq(scala212, scala213))
  .jsSettings(coverageEnabled := false)
  .dependsOn(core, testkit % Test)

val circeVersion = "0.14.1"
lazy val circe = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full).in(file("circe"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-circe",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core"    % circeVersion,
      "io.circe" %%% "circe-parser"  % circeVersion
    )
  )
  .jsSettings(
    coverageEnabled := false
  )
  .dependsOn(core, testkit % Test)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  Test / publishArtifact := false,
  // The Nexus repo we're publishing to.
  publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
  ),
  pomIncludeRepository := { x => false },
  pomExtra := (
    <scm>
      <url>https://github.com/gnieh/diffson</url>
      <connection>scm:git:git://github.com/gnieh/diffson.git</connection>
      <developerConnection>scm:git:git@github.com:gnieh/diffson.git</developerConnection>
      <tag>HEAD</tag>
    </scm>
    <developers>
      <developer>
        <id>satabin</id>
        <name>Lucas Satabin</name>
        <email>lucas.satabin@gnieh.org</email>
      </developer>
    </developers>
    <ciManagement>
      <system>travis</system>
      <url>https://travis-ci.org/#!/gnieh/diffson</url>
    </ciManagement>
    <issueManagement>
      <system>github</system>
      <url>https://github.com/gnieh/diffson/issues</url>
    </issueManagement>
  )
)

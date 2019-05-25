import scalariform.formatter.preferences._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val scala211 = "2.11.12"
val scala212 = "2.12.8"
val scala213 = "2.13.0-M5"

lazy val commonSettings = Seq(
  organization := "org.gnieh",
  scalaVersion := scala212,
  version := "4.0.0-M3",
  description := "Json diff/patch library",
  licenses += ("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/gnieh/diffson")),
  parallelExecution := false,
  scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
    case Some((2, n)) if n >= 13 =>
      Seq(
        "-Ymacro-annotations"
      )
  }.toList.flatten,
  libraryDependencies ++=
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq(
          compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
        )
      case _ =>
        // if scala 2.13.0-M4 or later, macro annotations merged into scala-reflect
        Nil
    }),
  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.10" cross CrossVersion.binary),
  scalariformAutoformat := true,
  scalariformPreferences := {
    scalariformPreferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
  },
  coverageExcludedPackages := "<empty>;.*Test.*",
  scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits"),
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
  .settings(
    name := "diffson",
    packagedArtifacts := Map())
  .aggregate(core.jvm, core.js, sprayJson, circe.jvm, circe.js, playJson.jvm, playJson.js, testkit.jvm, testkit.js)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure).in(file("core"))
  .enablePlugins(ScoverageSbtPlugin, ScalaUnidocPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-core",
    crossScalaVersions := Seq(scala211, scala212/*, scala213*/),
    libraryDependencies ++= Seq(
      "org.typelevel"  %%% "cats-core"  % "1.6.0",
      "io.estatico"    %%% "newtype"    % "0.4.2",
      "org.scalatest"  %%% "scalatest"  % "3.1.0-SNAP11" % Test,
      "org.scalacheck" %%% "scalacheck" % "1.14.0"      % Test
    ))
  .jsSettings(coverageEnabled := false)

lazy val testkit = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full).in(file("testkit"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-testkit",
    crossScalaVersions := Seq(scala211, scala212/*, scala213*/),
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.1.0-SNAP11",
      "org.scalacheck" %%% "scalacheck" % "1.14.0"))
  .jsSettings(coverageEnabled := false)
  .dependsOn(core)

lazy val sprayJson = project.in(file("sprayJson"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-spray-json",
    crossScalaVersions := Seq(scala211, scala212),
    libraryDependencies += "io.spray" %%  "spray-json" % "1.3.5")
  .dependsOn(core.jvm, testkit.jvm % Test)

lazy val playJson = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full).in(file("playJson"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-play-json",
    libraryDependencies += "com.typesafe.play" %%% "play-json" % "2.7.3",
    crossScalaVersions := Seq(scala211, scala212))
  .jsSettings(coverageEnabled := false)
  .dependsOn(core, testkit % Test)

val circeVersion = "0.11.1"
lazy val circe = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full).in(file("circe"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-circe",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core"    % circeVersion,
      "io.circe" %%% "circe-parser"  % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion % Test
    ),
    crossScalaVersions := Seq(scala211, scala212))
  .jsSettings(coverageEnabled := false)
  .dependsOn(core, testkit % Test)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
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

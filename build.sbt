import scalariform.formatter.preferences._

val scala211 = "2.11.12"
val scala212 = "2.12.8"
val scala213 = "2.13.0-M5"

lazy val commonSettings = Seq(
  organization := "org.gnieh",
  scalaVersion := scala212,
  version := "4.0.0-SNAPSHOT",
  description := "Json diff/patch library",
  licenses += ("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/gnieh/diffson")),
  parallelExecution := false,
  scalariformAutoformat := true,
  scalariformPreferences := {
    scalariformPreferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
  },
  fork in test := true,
  scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits"),
  autoAPIMappings := true,
  OsgiKeys.exportPackage := Seq("gnieh.diffson"),
  OsgiKeys.privatePackage := Seq(),
  resourceDirectories in Compile := List(),
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")) ++ Seq(
    scalariformPreferences := {
    scalariformPreferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
      .setPreference(DanglingCloseParenthesis, Prevent)
    }) ++ publishSettings

lazy val diffson = project.in(file("."))
  .enablePlugins(SbtOsgi, ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson",
    packagedArtifacts := Map())
  .aggregate(core)//, sprayJson, playJson, circe)

lazy val core = project.in(file("core"))
  .enablePlugins(SbtOsgi, ScoverageSbtPlugin, ScalaUnidocPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-core",
    crossScalaVersions := Seq(scala211, scala212, scala213),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.6.0",
      "org.scalatest" %% "scalatest" % "3.1.0-SNAP7" % Test,
      "org.scalacheck" %% "scalacheck" % "1.14.0" % Test),
    OsgiKeys.additionalHeaders := Map (
      "Bundle-Name" -> "Gnieh Diffson Core"
    ),
    OsgiKeys.bundleSymbolicName := "org.gnieh.diffson.core")

//lazy val sprayJson = project.in(file("sprayJson"))
//  .enablePlugins(SbtOsgi, ScoverageSbtPlugin)
//  .settings(commonSettings: _*)
//  .settings(
//    name := "diffson-spray-json",
//    crossScalaVersions := Seq(scala211, scala212),
//    libraryDependencies += "io.spray" %%  "spray-json" % "1.3.4",
//    OsgiKeys.additionalHeaders := Map (
//      "Bundle-Name" -> "Gnieh Diffson Spray Json"
//    ),
//    OsgiKeys.bundleSymbolicName := "org.gnieh.diffson.spray")
//  .dependsOn(core % "test->test;compile->compile")
//
//lazy val playJson = project.in(file("playJson"))
//  .enablePlugins(SbtOsgi, ScoverageSbtPlugin)
//  .settings(commonSettings: _*)
//  .settings(
//    name := "diffson-play-json",
//    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.10",
//    crossScalaVersions := Seq(scala211, scala212),
//    OsgiKeys.additionalHeaders := Map (
//      "Bundle-Name" -> "Gnieh Diffson Play! Json"
//    ),
//    OsgiKeys.bundleSymbolicName := "org.gnieh.diffson.play")
//  .dependsOn(core % "test->test;compile->compile")
//
//val circeVersion = "0.10.0"
//lazy val circe = project.in(file("circe"))
//  .enablePlugins(SbtOsgi, ScoverageSbtPlugin)
//  .settings(commonSettings: _*)
//  .settings(
//    name := "diffson-circe",
//    libraryDependencies ++= Seq(
//      "io.circe" %% "circe-core"    % circeVersion,
//      "io.circe" %% "circe-parser"  % circeVersion,
//      "io.circe" %% "circe-generic" % circeVersion % "test"
//    ),
//    crossScalaVersions := Seq(scala211, scala212),
//    OsgiKeys.additionalHeaders := Map (
//      "Bundle-Name" -> "Gnieh Diffson Circe"
//    ),
//    OsgiKeys.bundleSymbolicName := "org.gnieh.diffson.circe")
//  .dependsOn(core % "test->test;compile->compile")

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

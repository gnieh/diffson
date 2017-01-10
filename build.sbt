import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

import UnidocKeys._

val scala210 = "2.10.6"
val scala211 = "2.11.8"
val scala212 = "2.12.1"

lazy val commonSettings = Seq(
  organization := "org.gnieh",
  scalaVersion := scala212,
  version := "2.2.0-SNAPSHOT",
  description := "Json diff/patch library",
  licenses += ("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/gnieh/diffson")),
  crossScalaVersions := Seq(scala210, scala211, scala212),
  parallelExecution := false,
  fork in test := true,
  scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits"),
  autoAPIMappings := true,
  OsgiKeys.exportPackage := Seq("gnieh.diffson"),
  OsgiKeys.privatePackage := Seq(),
  resourceDirectories in Compile := List(),
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")) ++ scalariformSettings ++ Seq(
    ScalariformKeys.preferences := {
    ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
    }) ++ publishSettings

lazy val diffson = project.in(file("."))
  .enablePlugins(SbtOsgi, ScoverageSbtPlugin, CrossPerProjectPlugin)
  .settings(commonSettings: _*)
  .settings(unidocSettings: _*)
  .settings(
    name := "diffson",
    packagedArtifacts := Map())
  .aggregate(core, sprayJson, playJson, circe)

lazy val core = project.in(file("core"))
  .enablePlugins(SbtOsgi, ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-core",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.0" % "test",
      "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"),
    OsgiKeys.additionalHeaders := Map (
      "Bundle-Name" -> "Gnieh Diffson Core"
    ),
    OsgiKeys.bundleSymbolicName := "org.gnieh.diffson.core")

lazy val sprayJson = project.in(file("sprayJson"))
  .enablePlugins(SbtOsgi, ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-spray-json",
    libraryDependencies += "io.spray" %%  "spray-json" % "1.3.2",
    OsgiKeys.additionalHeaders := Map (
      "Bundle-Name" -> "Gnieh Diffson Spray Json"
    ),
    OsgiKeys.bundleSymbolicName := "org.gnieh.diffson.spray")
  .dependsOn(core % "test->test;compile->compile")

lazy val playJson = project.in(file("playJson"))
  .enablePlugins(SbtOsgi, ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-play-json",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0-M1",
    crossScalaVersions -= scala210,
    OsgiKeys.additionalHeaders := Map (
      "Bundle-Name" -> "Gnieh Diffson Play! Json"
    ),
    OsgiKeys.bundleSymbolicName := "org.gnieh.diffson.play")
  .dependsOn(core % "test->test;compile->compile")

val circeVersion = "0.6.0"
lazy val circe = project.in(file("circe"))
  .enablePlugins(SbtOsgi, ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-circe",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion % "test"
    ),
    crossScalaVersions -= scala210,
    OsgiKeys.additionalHeaders := Map (
      "Bundle-Name" -> "Gnieh Diffson Circe"
    ),
    OsgiKeys.bundleSymbolicName := "org.gnieh.diffson.circe")
  .dependsOn(core % "test->test;compile->compile")

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  // The Nexus repo we're publishing to.
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
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

package diffson

import sbt._
import Keys._
import com.typesafe.sbt.osgi.SbtOsgi._
import com.typesafe.sbt.osgi.OsgiKeys

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform._

object DiffsonBuild extends Build {

  lazy val diffson = (Project(id = "diffson",
    base = file(".")) settings (
    organization := "org.gnieh",
    name := "diffson",
    version := "1.0.0",
    scalaVersion := "2.11.6",
    description := "Json diff/patch library",
    licenses += ("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage := Some(url("https://github.com/gnieh/diffson")),
    crossScalaVersions := Seq("2.11.6", "2.10.5"),
    libraryDependencies ++= dependencies,
    parallelExecution := false,
    fork in test := true)
    settings(osgiSettings: _*)
    settings(
    resourceDirectories in Compile := List(),
    OsgiKeys.exportPackage := Seq(
      "gnieh.diffson"
    ),
    OsgiKeys.additionalHeaders := Map (
      "Bundle-Name" -> "Gnieh Diffson"
    ),
    OsgiKeys.bundleSymbolicName := "org.gnieh.diffson",
    OsgiKeys.privatePackage := Seq(),
    compileOptions)
    settings(publishSettings: _*)
    settings(scalariformSettings: _*)
    settings(
      ScalariformKeys.preferences := ScalariformKeys.preferences.value
        .setPreference(AlignSingleLineCaseStatements, true)
        .setPreference(DoubleIndentClassDeclaration, true)
        .setPreference(MultilineScaladocCommentsStartOnFirstLine, true))

  )

  lazy val dependencies = Seq(
    "org.scalatest" %% "scalatest" % "2.2.0" % "test",
    "io.spray" %%  "spray-json" % "1.3.1"
  )

  lazy val compileOptions = scalacOptions ++=
    Seq("-deprecation", "-language:_")

  lazy val publishSettings = Seq(
    publishMavenStyle := true,
    publishArtifact in Test := false,
    // The Nexus repo we're publishing to.
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
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

}

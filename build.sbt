import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

lazy val diffson = (project in file(".")).
  enablePlugins(SbtOsgi, ScoverageSbtPlugin).
  settings(
    organization := "org.gnieh",
    name := "diffson",
    version := "2.1.0-SNAPSHOT",
    scalaVersion := "2.11.8",
    description := "Json diff/patch library",
    licenses += ("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage := Some(url("https://github.com/gnieh/diffson")),
    crossScalaVersions := Seq("2.11.8"),
    coverageExcludedPackages := "<empty>;gnieh\\.diffson\\.playJson\\..*",
    libraryDependencies ++= dependencies,
    parallelExecution := false,
    fork in test := true).
  settings(
    resourceDirectories in Compile := List(),
    OsgiKeys.exportPackage := Seq(
      "gnieh.diffson",
      "gnieh.diffson.playJson",
      "gnieh.diffson.sprayJson"
    ),
    OsgiKeys.additionalHeaders := Map (
      "Bundle-Name" -> "Gnieh Diffson"
    ),
    OsgiKeys.bundleSymbolicName := "org.gnieh.diffson",
    OsgiKeys.privatePackage := Seq()).
  settings(
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
    scalacOptions in (Compile, doc) ++= Seq("-implicits", "-implicits-show-all", "-diagrams")).
  settings(publishSettings).
  settings(scalariformSettings).
  settings(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true))

lazy val dependencies = Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.0" % "test",
  "io.spray" %%  "spray-json" % "1.3.2" % "provided,test",
  "com.typesafe.play" %% "play-json" % "2.5.2" % "provided"
)

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



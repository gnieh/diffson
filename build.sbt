import com.typesafe.tools.mima.core._

val scala212 = "2.12.18"
val scala213 = "2.13.12"
val scala3 = "3.3.1"

val scalatestVersion = "3.2.17"
val scalacheckVersion = "1.17.0"

ThisBuild / tlJdkRelease := Some(11)
ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq(elems = scala212, scala213, scala3)

ThisBuild / tlFatalWarnings := false

ThisBuild / tlBaseVersion := "4.4"

ThisBuild / organization := "org.gnieh"
ThisBuild / organizationName := "Lucas Satabin"
ThisBuild / startYear := Some(2022)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  tlGitHubDev("satabin", "Lucas Satabin")
)

lazy val commonSettings = Seq(
  description := "Json diff/patch library",
  homepage := Some(url("https://github.com/gnieh/diffson"))
)

lazy val diffson = tlCrossRootProject.aggregate(core, sprayJson, circe, playJson, testkit)

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .enablePlugins(ScalaUnidocPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-core",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.11.0",
      "org.typelevel" %%% "cats-core" % "2.10.0",
      "org.scalatest" %%% "scalatest" % scalatestVersion % Test,
      "org.scalacheck" %%% "scalacheck" % scalacheckVersion % Test
    ),
    mimaBinaryIssueFilters ++= List(
      ProblemFilters.exclude[DirectMissingMethodProblem](
        "diffson.jsonpatch.package#simplediff#remembering.JsonDiffDiff"),
      ProblemFilters.exclude[DirectAbstractMethodProblem]("diffson.lcs.Lcs.savedHashes")
    )
  )

lazy val testkit = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("testkit"))
  .settings(commonSettings: _*)
  .settings(name := "diffson-testkit",
            libraryDependencies ++= Seq("org.scalatest" %%% "scalatest" % scalatestVersion,
                                        "org.scalacheck" %%% "scalacheck" % scalacheckVersion))
  .dependsOn(core)

lazy val sprayJson = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("sprayJson"))
  .settings(commonSettings: _*)
  .settings(name := "diffson-spray-json",
            libraryDependencies += "io.spray" %% "spray-json" % "1.3.6",
            tlVersionIntroduced := Map("3" -> "4.5.0"))
  .dependsOn(core, testkit % Test)

lazy val playJson = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("playJson"))
  .settings(commonSettings: _*)
  .settings(name := "diffson-play-json",
            libraryDependencies += "org.playframework" %%% "play-json" % "3.0.1",
            tlVersionIntroduced := Map("3" -> "4.3.0"))
  .nativeSettings(tlVersionIntroduced := Map("2.12" -> "4.5.0", "2.13" -> "4.5.0", "3" -> "4.5.0"))
  .dependsOn(core, testkit % Test)

val circeVersion = "0.14.6"
lazy val circe = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("circe"))
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-circe",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion
    )
  )
  .dependsOn(core, testkit % Test)

lazy val benchmarks = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("benchmarks"))
  .enablePlugins(NoPublishPlugin, JmhPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "diffson-benchmarks",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-literal" % circeVersion
    )
  )
  .dependsOn(circe)

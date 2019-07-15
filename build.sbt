// See LICENSE.Berkeley for license details.

import sbt.complete._
import sbt.complete.DefaultParsers._
import xerial.sbt.pack._
import sys.process._

enablePlugins(PackPlugin)

lazy val commonSettings = Seq(
  organization := "edu.berkeley.cs",
  version      := "1.2-SNAPSHOT",
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.12.4"),
  parallelExecution in Global := false,
  traceLevel   := 15,
  scalacOptions ++= Seq("-deprecation","-unchecked","-Xsource:2.11"),
  libraryDependencies ++= Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value),
  libraryDependencies ++= Seq("org.json4s" %% "json4s-jackson" % "3.6.1"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { x => false },
  pomExtra := <url>https://github.com/freechipsproject/rocket-chip</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
    <license>
      <name>BSD-style</name>
        <url>http://www.opensource.org/licenses/bsd-license.php</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/freechipsproject/rocketchip.git</url>
      <connection>scm:git:github.com/freechipsproject/rocketchip.git</connection>
    </scm>,
  publishTo := {
    val v = version.value
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) {
      Some("snapshots" at nexus + "content/repositories/snapshots")
    }
    else {
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
  }
)

lazy val chisel = (project in file("chisel3")).settings(commonSettings)

def dependOnChisel(prj: Project) = {
  if (sys.props.contains("ROCKET_USE_MAVEN")) {
    prj.settings(
      libraryDependencies ++= Seq("edu.berkeley.cs" %% "chisel3" % "3.2-SNAPSHOT")
    )
  } else {
    prj.dependsOn(chisel)
  }
}

lazy val hardfloat  = dependOnChisel(project).settings(commonSettings)
  .settings(crossScalaVersions := Seq("2.12.4"))
lazy val posit  = dependOnChisel(project).settings(commonSettings)
  .settings(crossScalaVersions := Seq("2.12.4"))
lazy val `rocket-macros` = (project in file("macros")).settings(commonSettings)
lazy val rocketchip = dependOnChisel(project in file("."))
  .settings(commonSettings, chipSettings)
  .dependsOn(hardfloat, `rocket-macros`)
  .aggregate(hardfloat, `rocket-macros`) // <-- means the running task on rocketchip is also run by aggregate tasks

lazy val addons = settingKey[Seq[String]]("list of addons used for this build")
lazy val make = inputKey[Unit]("trigger backend-specific makefile command")
val setMake = NotSpace ~ ( Space ~> NotSpace )

lazy val chipSettings = Seq(
  addons := {
    val a = sys.env.getOrElse("ROCKETCHIP_ADDONS", "posit")
    println(s"Using addons: $a")
    a.split(" ")
  },
  unmanagedSourceDirectories in Compile ++= addons.value.map(baseDirectory.value / _ / "src/main/scala"),
  mainClass in (Compile, run) := Some("rocketchip.Generator"),
  make := {
    val jobs = java.lang.Runtime.getRuntime.availableProcessors
    val (makeDir, target) = setMake.parsed
    (run in Compile).evaluated
    s"make -C $makeDir  -j $jobs $target".!
  }
)


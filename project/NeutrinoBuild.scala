import sbt._
import sbt.Classpaths.publishTask
import Keys._

import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.{ MultiJvm, extraOptions, jvmOptions, scalatestOptions, multiNodeExecuteTests, multiNodeJavaName, multiNodeHostsFileName, multiNodeTargetDirName, multiTestOptions }
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import sbtassembly.AssemblyPlugin.autoImport._

import com.twitter.scrooge.ScroogeSBT

import com.typesafe.sbt.SbtNativePackager._, autoImport._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd, CmdLike}

import com.goshoplane.sbt.standard.libraries.StandardLibraries

object NeutrinoBuild extends Build with StandardLibraries {

  lazy val makeScript = TaskKey[Unit]("make-script", "make script in local directory to run main classes")

  def sharedSettings = Seq(
    organization := "com.goshoplane",
    version := "0.0.1",
    scalaVersion := Version.scala,
    crossScalaVersions := Seq(Version.scala, "2.11.4"),
    scalacOptions := Seq("-unchecked", "-optimize", "-deprecation", "-feature", "-language:higherKinds", "-language:implicitConversions", "-language:postfixOps", "-language:reflectiveCalls", "-Yinline-warnings", "-encoding", "utf8"),
    retrieveManaged := true,

    fork := true,
    javaOptions += "-Xmx2500M",

    resolvers ++= StandardResolvers,

    publishMavenStyle := true
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings


  lazy val neutrino = Project(
    id = "neutrino",
    base = file("."),
    settings = Project.defaultSettings ++
      sharedSettings
  ).settings(
  ) aggregate (core, auth, user, service)


  lazy val core = Project(
    id = "neutrino-core",
    base = file("core"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      ScroogeSBT.newSettings
  ).settings(
    name := "neutrino-core",

    libraryDependencies ++= Seq(
    ) ++ Libs.scalaz
      ++ Libs.akka
      ++ Libs.scaldi
  )


  lazy val auth = Project(
    id = "neutrino-auth",
    base = file("auth"),
    settings = Project.defaultSettings ++
      sharedSettings
  ).settings(
    name := "neutrino-auth",

    libraryDependencies ++= Seq(
    ) ++ Libs.scalaz
      ++ Libs.akka
      ++ Libs.scaldi
      ++ Libs.restFb
      ++ Libs.playJson
      ++ Libs.phantom
      ++ Libs.googleApiClient
      ++ Libs.googleHttpClient
      ++ Libs.googleApisOAuth2
  ).dependsOn(core, user)


  lazy val user = Project(
    id = "neutrino-user",
    base = file("user"),
    settings = Project.defaultSettings ++
      sharedSettings
    ).settings(
      name := "neutrino-user",

      libraryDependencies ++= Seq(
      ) ++ Libs.scalaz
        ++ Libs.akka
        ++ Libs.scaldi
        ++ Libs.phantom
        ++ Libs.commonsCore
    ).dependsOn(core)


  lazy val service = Project(
    id = "neutrino-service",
    base = file("service"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      ScroogeSBT.newSettings
  ).enablePlugins(JavaAppPackaging)
  .settings(
    name := "neutrino-service",

    libraryDependencies ++= Seq(
    ) ++ Libs.scalaz
      ++ Libs.akka
      ++ Libs.scaldi
      ++ Libs.microservice
      ++ Seq("com.typesafe.akka" %% "akka-cluster" % "2.4.0"),

    makeScript <<= (stage in Universal, stagingDirectory in Universal, baseDirectory in ThisBuild, streams) map { (_, dir, cwd, streams) =>
      var path = dir / "bin" / "neutrino-service"
      sbt.Process(Seq("ln", "-sf", path.toString, "neutrino-service"), cwd) ! streams.log
    }
  ).dependsOn(auth)

}
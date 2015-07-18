import sbt._
import sbt.Classpaths.publishTask
import Keys._

import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.{ MultiJvm, extraOptions, jvmOptions, scalatestOptions, multiNodeExecuteTests, multiNodeJavaName, multiNodeHostsFileName, multiNodeTargetDirName, multiTestOptions }
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

// import com.typesafe.sbt.SbtStartScript

import sbtassembly.AssemblyPlugin.autoImport._

import com.twitter.scrooge.ScroogeSBT

import com.typesafe.sbt.SbtNativePackager._, autoImport._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd, CmdLike}

object NeutrinoBuild extends Build with Libraries {

  def sharedSettings = Seq(
    organization := "com.goshoplane",
    version := "0.0.1",
    scalaVersion := Version.scala,
    crossScalaVersions := Seq(Version.scala, "2.11.4"),
    scalacOptions := Seq("-unchecked", "-optimize", "-deprecation", "-feature", "-language:higherKinds", "-language:implicitConversions", "-language:postfixOps", "-language:reflectiveCalls", "-Yinline-warnings", "-encoding", "utf8"),
    retrieveManaged := true,

    fork := true,
    javaOptions += "-Xmx2500M",

    resolvers ++= Seq(
      "ReaderDeck Releases"    at "http://repo.readerdeck.com/artifactory/readerdeck-releases",
      "anormcypher"            at "http://repo.anormcypher.org/",
      "Akka Repository"        at "http://repo.akka.io/releases",
      "Spray Repository"       at "http://repo.spray.io/",
      "twitter-repo"           at "http://maven.twttr.com",
      "Typesafe Repository"    at "http://repo.typesafe.com/typesafe/releases/",
      "Websudos releases"      at "http://maven.websudos.co.uk/ext-release-local",
      "Websudos snapshots"     at "http://maven.websudos.co.uk/ext-snapshot-local",
      "Sonatype repo"          at "https://oss.sonatype.org/content/groups/scala-tools/",
      "Sonatype releases"      at "https://oss.sonatype.org/content/repositories/releases",
      "Sonatype snapshots"     at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype staging"       at "http://oss.sonatype.org/content/repositories/staging",
      "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"
    ),

    publishMavenStyle := true
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings


  lazy val neutrino = Project(
    id = "neutrino",
    base = file("."),
    settings = Project.defaultSettings ++
      sharedSettings
  ).settings(
  ) aggregate (core, user, bucket, feed, shopplan, search, service)



  lazy val core = Project(
    id = "neutrino-core",
    base = file("core"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      // SbtStartScript.startScriptForClassesSettings ++
      ScroogeSBT.newSettings
  ).settings(
    name := "neutrino-core",

    libraryDependencies ++= Seq(
    ) ++ Libs.scalaz
      ++ Libs.scroogeCore
      ++ Libs.finagleThrift
      ++ Libs.libThrift
      ++ Libs.akka
      ++ Libs.scaldi
      ++ Libs.fastutil
      ++ Libs.catalogueCommons
  )



  lazy val user = Project(
    id = "neutrino-user",
    base = file("user"),
    settings = Project.defaultSettings ++
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
  ).settings(
    name := "neutrino-user",

    libraryDependencies ++= Seq(
    ) ++ Libs.akka
      ++ Libs.slf4j
      ++ Libs.logback
      ++ Libs.phantom
      ++ Libs.lapse
  ).dependsOn(core)


  lazy val bucket = Project(
    id = "neutrino-bucket",
    base = file("bucket"),
    settings = Project.defaultSettings ++
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
  ).settings(
    name := "neutrino-bucket",

    libraryDependencies ++= Seq(
    ) ++ Libs.akka
      ++ Libs.slf4j
      ++ Libs.logback
      ++ Libs.phantom
      ++ Libs.bijection
  ).dependsOn(core)



  lazy val feed = Project(
    id = "neutrino-feed",
    base = file("feed"),
    settings = Project.defaultSettings ++
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
  ).settings(
    name := "neutrino-feed",

    libraryDependencies ++= Seq(
    ) ++ Libs.akka
      ++ Libs.slf4j
      ++ Libs.logback
      ++ Libs.phantom
  ).dependsOn(core)


  lazy val search = Project(
    id = "neutrino-search",
    base = file("search"),
    settings = Project.defaultSettings ++
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
  ).settings(
    name := "neutrino-search",

    libraryDependencies ++= Seq(
    ) ++ Libs.akka
      ++ Libs.slf4j
      ++ Libs.logback
      ++ Libs.bijection
      ++ Libs.playJson
  ).dependsOn(core)


  lazy val shopplan = Project(
    id = "neutrino-shopplan",
    base = file("shopplan"),
    settings = Project.defaultSettings ++
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
  ).settings(
    name := "neutrino-shopplan",

    libraryDependencies ++= Seq(
    ) ++ Libs.akka
      ++ Libs.slf4j
      ++ Libs.logback
      ++ Libs.phantom
      ++ Libs.playJson
      ++ Libs.googleMaps
  ).dependsOn(core, user, bucket)


  lazy val service = Project(
    id = "neutrino-service",
    base = file("service"),
    settings = Project.defaultSettings ++
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
  ).enablePlugins(JavaAppPackaging)
  .settings(
    name := "neutrino-service",
    mainClass in Compile := Some("neutrino.service.NeutrinoServer"),

    dockerExposedPorts := Seq(2424),
    // TODO: remove echo statement once verified
    dockerEntrypoint := Seq("sh", "-c", "export NEUTRINO_HOST=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1 }'` && echo $NEUTRINO_HOST && bin/neutrino-service $*"),
    dockerRepository := Some("docker"),
    dockerBaseImage := "shoplane/baseimage",
    dockerCommands ++= Seq(
      Cmd("USER", "root")
    ),
    libraryDependencies ++= Seq(
    ) ++ Libs.akka
      ++ Libs.slf4j
      ++ Libs.logback
      ++ Libs.finagleCore
      ++ Libs.scalaJLine
      ++ Libs.mimepull
      ++ Libs.scaldi
      ++ Libs.scaldiAkka
      ++ Libs.bijection
      ++ Libs.lapse
  ).dependsOn(core, user, bucket, feed, shopplan, search)

}
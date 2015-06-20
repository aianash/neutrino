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
import com.typesafe.sbt.packager.Keys.{executableScriptName => _, _}
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
      "anormcypher"            at "http://repo.anormcypher.org/",
      "Akka Repository"        at "http://repo.akka.io/releases",
      "Spray Repository"       at "http://repo.spray.io/",
      "twitter-repo"           at "http://maven.twttr.com",
      "Typesafe Repository"    at "http://repo.typesafe.com/typesafe/releases/",
      // This repo is not really working, so commented
      // "Websudos releases"      at "http://maven.websudos.co.uk/ext-release-local",
      // "Websudos snapshots"     at "http://maven.websudos.co.uk/ext-snapshot-local",
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
  ) aggregate (core, user, bucket, feed, shopplan, search, service, scripts)



  lazy val core = Project(
    id = "neutrino-core",
    base = file("core"),
    settings = Project.defaultSettings ++
      sharedSettings
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


  // makeScript task is used to create a linked scripts
  // while development
  lazy val makeScript = TaskKey[Unit]("make-script", "make script to run scripts")


  lazy val service = Project(
    id = "neutrino-service",
    base = file("service"),
    settings = Project.defaultSettings ++
      sharedSettings
  ).enablePlugins(JavaAppPackaging)
  .settings(
    name := "neutrino-service",
    mainClass in Compile := Some("neutrino.service.NeutrinoServer"),

    makeScript <<= (stage in Universal, stagingDirectory in Universal, baseDirectory in ThisBuild, streams) map { (_, dir, cwd, streams) =>
      var path = dir / "bin" / "neutrino-service"
      sbt.Process(Seq("ln", "-sf", path.toString, "neutrino-service"), cwd) ! streams.log
    },


    dockerExposedPorts := Seq(2424),
    // TODO: remove echo statement once verified
    dockerEntrypoint := Seq("sh", "-c", "export NEUTRINO_HOST=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1 }'` && echo $NEUTRINO_HOST && bin/neutrino-service $*"),
    dockerRepository := Some("docker"),
    dockerBaseImage := "phusion/baseimage",
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      new CmdLike {
        def makeContent = """|RUN \
                             |  echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
                             |  add-apt-repository -y ppa:webupd8team/java && \
                             |  apt-get update && \
                             |  apt-get install -y oracle-java7-installer && \
                             |  rm -rf /var/lib/apt/lists/* && \
                             |  rm -rf /var/cache/oracle-jdk7-installer""".stripMargin
      }
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


  lazy val scripts = Project(
    id = "neutrino-scripts",
    base = file("scripts"),
    settings = Project.defaultSettings ++
      sharedSettings
  ).enablePlugins(JavaAppPackaging)
  .settings(
    name := "neutrino-scripts",
    mainClass in Compile := Some("neutrino.scripts.NeutrinoClient"),

    makeScript <<= (stage in Universal, stagingDirectory in Universal, baseDirectory in ThisBuild, streams) map { (_, dir, cwd, streams) =>
      var path = dir / "bin" / "neutrino-client"
      sbt.Process(Seq("ln", "-sf", path.toString, "neutrino-client"), cwd) ! streams.log
    },

    executableScriptName := "neutrino-client",

    libraryDependencies ++= Seq(
    ) ++ Libs.catalogueCommons
      ++ Libs.lapse
  ).dependsOn(core)

}
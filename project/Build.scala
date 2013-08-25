import sbt._

import Keys._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    name := "Palindrompit",
    version := "0.1",
    versionCode := 0,
    scalaVersion := "2.10.2",
    platformName in Android := "android-16",
    javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6"),
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.0"
  )

  val proguardSettings = Seq (
    useProguard in Android := true
  )

  lazy val fullAndroidSettings =
    General.settings ++
    AndroidProject.androidSettings ++
    TypedResources.settings ++
    proguardSettings ++
    AndroidManifestGenerator.settings ++
    AndroidMarketPublish.settings ++ Seq (
      keyalias in Android := "change-me"
    )
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "Palindrompit",
    file("."),
    settings = General.fullAndroidSettings
  )
}

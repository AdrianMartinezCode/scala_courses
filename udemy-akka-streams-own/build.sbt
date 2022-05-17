version := "0.1.0-SNAPSHOT"
//scalaVersion := "3.0.2"

//lazy val root = (project in file("."))
//  .settings(
//    name := "udemy-akka-streams-own"
//  )

//name := "ude"
scalaVersion := "2.12.8"

lazy val akkaVersion = "2.5.19"
lazy val scalaTestVersion = "3.0.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion
)
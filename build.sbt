name := "gitbucket-notifications-plugin"

organization := "io.github.gitbucket"
version := "1.0.0"
scalaVersion := "2.12.2"

lazy val root = (project in file(".")).enablePlugins(SbtTwirl)

libraryDependencies ++= Seq(
  "io.github.gitbucket" %% "gitbucket"         % "4.15.0-SNAPSHOT" % "provided",
  "javax.servlet"        % "javax.servlet-api" % "3.1.0" % "provided"
)

scalacOptions ++= Seq("-feature", "-deprecation")
useJCenter := true

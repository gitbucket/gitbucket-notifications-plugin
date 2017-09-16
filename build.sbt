val Organization = "io.github.gitbucket"
val ProjectName = "gitbucket-notifications-plugin"
val ProjectVersion = "1.2.0"
val GitBucketVersion = Option(System.getProperty("gitbucket.version")).getOrElse("4.17.0-SNAPSHOT")

name := ProjectName
organization := Organization
version := ProjectVersion
scalaVersion := "2.12.2"

lazy val root = (project in file(".")).enablePlugins(SbtTwirl)

libraryDependencies ++= Seq(
  "io.github.gitbucket" %% "gitbucket"         % GitBucketVersion % "provided",
  "javax.servlet"        % "javax.servlet-api" % "3.1.0"          % "provided"
)

scalacOptions ++= Seq("-feature", "-deprecation")
useJCenter := true

name := "framework"

version := "0.1"

scalaVersion := "2.12.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature", "-Xfatal-warnings",
  "-Yno-adapted-args", "-explaintypes")

resolvers ++= Seq(
  Resolver.typesafeIvyRepo("releases"),
  Resolver.bintrayRepo("hmrc", "releases"))

libraryDependencies ++= Seq(
  // Application configuration
  "com.typesafe"                     %  "config"                  % "1.3.3",

  // Akka
  "com.typesafe.akka"                %% "akka-http"               % "10.1.3",
  "com.typesafe.akka"                %% "akka-stream"             % "2.5.13",
  "com.typesafe.akka"                %% "akka-slf4j"              % "2.5.13",

  // Logging
  "org.slf4j"                        %  "slf4j-api"               % "1.7.25",
  "org.slf4j"                        % "jcl-over-slf4j"           % "1.7.25",
  "org.slf4j"                        % "log4j-over-slf4j"         % "1.7.25",
  "ch.qos.logback"                   % "logback-classic"          % "1.2.3",
  "com.typesafe.scala-logging"       %% "scala-logging"           % "3.9.0",

  // Email
  "uk.gov.hmrc"                      %% "emailaddress"            % "2.2.0",

  // Json
  "com.fasterxml.jackson.core"       %  "jackson-databind"        % "2.9.5",
  "com.fasterxml.jackson.module"     %% "jackson-module-scala"    % "2.9.5",
  "com.fasterxml.jackson.datatype"   %  "jackson-datatype-jsr310" % "2.9.5")

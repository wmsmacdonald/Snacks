name := "SnaFoo"

version := "1.0"

lazy val `snafoo` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(cache , ws   , specs2 % Test, "org.xerial" % "sqlite-jdbc" % "3.8.11.2", "com.typesafe.play" %% "play-slick" % "2.0.0")

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"  
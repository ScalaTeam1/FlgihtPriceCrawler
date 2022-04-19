ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "WebCrawler"
  )

name := "WebCrawler"

version := "1.0"

scalaVersion := "2.13.4"

scalacOptions in(Compile, doc) ++= Seq("-groups", "-implicits", "-deprecation", "-Ywarn-dead-code", "-Ywarn-value-discard", "-Ywarn-unused" )
scalacOptions ++= Seq( "-encoding", "UTF-8")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.2" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.2",
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "io.spray" %%  "spray-json" % "1.3.6",
  "com.chuusai" %% "shapeless" % "2.3.3"
)
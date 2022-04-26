name := "WebCrawler"

version := "1.0"

scalaVersion := "2.12.8"

scalacOptions in (Compile, doc) ++= Seq(
  "-groups",
  "-implicits",
  "-deprecation",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)
scalacOptions ++= Seq("-encoding", "UTF-8")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.2" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.7.0",
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "io.spray" %% "spray-json" % "1.3.6",
  "com.chuusai" %% "shapeless" % "2.3.3",
  "com.typesafe" % "config" % "1.4.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "org.mongodb.scala" %% "mongo-scala-driver" % "4.2.3",
  "io.jvm.uuid" %% "scala-uuid" % "0.3.1",
  "org.zeroturnaround" % "zt-zip" % "1.15",
  "io.minio" % "minio" % "6.0.13",
  "commons-io" % "commons-io" % "2.5",
  "com.phasmidsoftware" %% "tableparser" % "1.0.14"
)

unmanagedBase := baseDirectory.value / "lib"

mainClass in (Compile, run) := Some(
  "edu.neu.coe.csye7200.flightPricePrediction.webCrawler.WebCrawler"
)

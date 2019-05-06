name := "test-http4s-auth"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions ++= Seq(
  "-language:higherKinds",
  "-Ypartial-unification"
)

libraryDependencies += "org.typelevel" %% "cats-core" % "1.6.0"
libraryDependencies += "org.typelevel" %% "cats-effect" % "1.2.0"
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.20.0"
libraryDependencies += "org.http4s" %% "http4s-blaze-client" % "0.20.0"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.20.0"
libraryDependencies += "org.http4s" %% "http4s-circe" % "0.20.0"
libraryDependencies +=  "org.http4s" %% "rho-swagger" % "0.19.0-M7" //FIXME: remove
libraryDependencies += "com.softwaremill.tapir" %% "tapir-core" % "0.7.6"
libraryDependencies += "com.softwaremill.tapir" %% "tapir-http4s-server" % "0.7.6"
libraryDependencies += "com.softwaremill.tapir" %% "tapir-json-circe" % "0.7.6"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.10.2"
libraryDependencies += "io.circe" %% "circe-core" % "0.11.1"
libraryDependencies += "io.circe" %% "circe-generic" % "0.11.1" 
libraryDependencies +="io.github.jmcardon" %% "tsec-jwt-mac" % "0.0.1-M11"

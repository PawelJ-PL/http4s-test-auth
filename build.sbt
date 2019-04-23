name := "test-http4s-auth"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions ++= Seq(
  "-language:higherKinds",
  "-Ypartial-unification"
)

libraryDependencies += "org.typelevel" %% "cats-core" % "1.6.0"
libraryDependencies += "org.typelevel" %% "cats-effect" % "1.2.0"
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.20.0-RC1"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.20.0-RC1"
libraryDependencies +=  "org.http4s" %% "rho-swagger" % "0.19.0-M7"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

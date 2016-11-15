name := "gobumapScripting"

version := "1.0"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.8.1",
  "org.tpolecat" %% "doobie-core-cats" % "0.3.1-M2",
  "org.tpolecat" %% "doobie-hikari-cats" % "0.3.1-M2"
)

mainClass in (Compile, run) := Some("edu.eckerd.scripting.gobumap.Application")

unmanagedBase := baseDirectory.value / "lib"
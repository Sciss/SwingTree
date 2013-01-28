name := "SwingTree"

version := "1.2.0"

organization := "de.sciss"

scalaVersion := "2.10.0"

scalacOptions ++= Seq("-deprecation", "-feature", "-language:higherKinds", "-language:implicitConversions")

libraryDependencies <+= scalaVersion { sv =>
  "org.scala-lang" % "scala-swing" % sv
}

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "junit" % "junit" % "4.11" % "test"
)

retrieveManaged := true

initialCommands in console := """
  import de.sciss.swingtree._
  import tree._
  import test._
  import Swing._
  import Tree._
"""

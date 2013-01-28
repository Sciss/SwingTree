name := "SwingTree"

version := "1.2.0"

organization := "de.sciss"

description := "A Scala Swing component that wraps javax.swing.JTree"

homepage <<= name { n => Some(url("https://github.com/Sciss/" + n)) }

licenses <<= name { n => Seq("BSD" -> url("https://raw.github.com/Sciss/" + n + "/master/LICENSE")) }

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

// ---- console ----

initialCommands in console := """
  import de.sciss.swingtree._
  import tree._
  import test._
  import swing.Swing._
  import Tree._
"""

// ---- build info ----

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
  BuildInfoKey.map(homepage) { case (k, opt) => k -> opt.get },
  BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
)

buildInfoPackage <<= (organization, name) { (o, n) => o + "." + n.toLowerCase }

// ---- publishing ----

publishMavenStyle := true

publishTo <<= version { (v: String) =>
   Some(if (v endsWith "-SNAPSHOT")
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
   else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
   )
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra <<= name { n =>
<scm>
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>
}

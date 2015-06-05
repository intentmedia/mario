name := "mario"

version := "0.1.0"

organization := "com.intentmedia.mario"

homepage := Some(url("https://github.com/intentmedia/mario"))

scalaVersion := "2.11.6"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
    <licenses>
      <license>
        <name>BSD-style</name>
        <url>http://www.opensource.org/licenses/bsd-license.php</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:intentmedia/mario.git</url>
      <connection>scm:git:git@github.com:intentmedia/mario.git</connection>
    </scm>
    <developers>
      <developer>
        <id>intentmedia</id>
        <name>Intent Media</name>
        <url>http://intentmedia.com</url>
      </developer>
    </developers>
  )
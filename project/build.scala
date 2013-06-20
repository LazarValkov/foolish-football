import sbt._
import Keys._
import net.virtualvoid.sbt.graph.Plugin._

// to sync this project with IntelliJ, run the sbt-idea plugin with: sbt gen-idea
object PatternsBuild extends Build {

  override val settings = super.settings ++ Seq(
    organization := "com.github.fommil.ff",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.10.2"
  )

  lazy val defaultSettings = Defaults.defaultSettings ++ graphSettings ++ Seq(
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-deprecation", "-unchecked"),
    javacOptions in Compile ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation", "-Xlint:-options"),
	// https://github.com/sbt/sbt/issues/702
	javaOptions += "-Djava.util.logging.config.file=logging.properties",
	javaOptions += "-Xmx2G",
	outputStrategy := Some(StdoutOutput),
	fork := true,
	maxErrors := 1,
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.sonatypeRepo("releases"),
      Resolver.typesafeRepo("releases"),
      Resolver.typesafeRepo("snapshots"),
      Resolver.sonatypeRepo("snapshots")
    ),
    parallelExecution in Test := false
  )

  def module(dir: String) = Project(id = dir, base = file(dir), settings = defaultSettings)
  import Dependencies._

  lazy val game = module("game") settings (
    libraryDependencies += java_logging,
	libraryDependencies += specs2 % "test"
  )

  lazy val analysis = module("analysis") dependsOn (game) settings (
	
  )

  lazy val root = Project(id = "parent", base = file("."), settings = defaultSettings) settings (
	mainClass in (Compile, run) := Some("com.github.fommil.ff.Main")
  ) dependsOn (game)

}

object Dependencies {
  // to help resolve transitive problems, type:
  //   `sbt dependency-graph`
  //   `sbt test:dependency-tree`
  val bad = Seq(
    ExclusionRule(name = "log4j"),
    ExclusionRule(name = "commons-logging"),
    ExclusionRule(organization = "org.slf4j")
  )

  val java_logging = "com.github.fommil" % "java-logging" % "1.0"
  val spring_core = "org.springframework" % "spring-core" % "3.1.4.RELEASE" excludeAll (bad: _*)
  val guava = "com.google.guava" % "guava" % "13.0.1" // includes Cache
  val jsr305 = "com.google.code.findbugs" % "jsr305" % "2.0.1" // undeclared dep of Guava
  val specs2 = "org.specs2" %% "specs2" % "1.13"
}
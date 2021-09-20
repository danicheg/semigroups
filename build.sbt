val Scala212 = "2.12.14"
val Scala213 = "2.13.6"
val Scala3 = "3.0.2"

ThisBuild / crossScalaVersions := Seq(Scala212, Scala213, Scala3)
ThisBuild / scalaVersion := Scala212
ThisBuild / githubWorkflowPublishTargetBranches := Seq()
ThisBuild / githubWorkflowJavaVersions := Seq("adopt@1.8")
ThisBuild / githubWorkflowArtifactUpload := false
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("coreJVM/mimaReportBinaryIssues", "coreJS/mimaReportBinaryIssues"), name = Some("Check binary issues")),
  WorkflowStep.Sbt(List("Test/compile"), name = Some("Compile")),
  WorkflowStep.Sbt(List("test"), name = Some("Run tests"))
)

def scalaVersionSpecificFolders(srcName: String, srcBaseDir: java.io.File, scalaVersion: String) = {
  def extraDirs(suffix: String) =
    List(CrossType.Pure, CrossType.Full)
      .flatMap(_.sharedSrcDir(srcBaseDir, srcName).toList.map(f => file(f.getPath + suffix)))

  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) => extraDirs("-2.x")
    case Some((0 | 3, _)) => extraDirs("-3.x")
    case _ => Nil
  }
}

lazy val semigroups = project.in(file("."))
  .settings(commonSettings, releaseSettings, skipOnPublishSettings)
  .settings(
    mimaPreviousArtifacts := Set()
  )
  .aggregate(coreJVM, coreJS)


lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(commonSettings, releaseSettings, mimaSettings)
  .settings(
    name := "semigroups"
  )
  .jsSettings(
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

val catsV = "2.6.1"
val disciplineMunitV = "1.0.9"
val kindProjectorV = "0.13.2"
val betterMonadicForV = "0.3.1"

lazy val contributors = Seq(
  "ChristopherDavenport" -> "Christopher Davenport"
)

// General Settings
lazy val commonSettings = Seq(
  organization := "io.chrisdavenport",
  Compile / unmanagedSourceDirectories ++= scalaVersionSpecificFolders(
    "main",
    baseDirectory.value,
    scalaVersion.value
  ),
  Test / unmanagedSourceDirectories ++= scalaVersionSpecificFolders(
    "test",
    baseDirectory.value,
    scalaVersion.value
  ),
  scalacOptions ++= (
    if (ScalaArtifacts.isScala3(scalaVersion.value)) Nil
    else Seq("-Yrangepos", "-language:higherKinds")
    ),
  libraryDependencies ++= (
    if (ScalaArtifacts.isScala3(scalaVersion.value)) Nil
    else
      Seq(
        compilerPlugin(
          ("org.typelevel" %% "kind-projector" % kindProjectorV).cross(CrossVersion.full)
        ),
        compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForV)
      )
    ),
  libraryDependencies ++= Seq(
    "org.typelevel"               %%% "cats-core"                  % catsV,
    "org.typelevel"               %%% "cats-laws"                  % catsV                % Test,
    "org.typelevel"               %%% "discipline-munit"           % disciplineMunitV     % Test
  )
)

lazy val releaseSettings = {
  import ReleaseTransformations._
  Seq(
    releaseCrossBuild := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      // For non cross-build projects, use releaseStepCommand("publishSigned")
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials ++= (
      for {
        username <- Option(System.getenv().get("SONATYPE_USERNAME"))
        password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
      } yield
        Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          username,
          password
        )
    ).toSeq,
    Test / publishArtifact := false,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ChristopherDavenport/semigroups"),
        "git@github.com:ChristopherDavenport/semigroups.git"
      )
    ),
    homepage := Some(url("https://github.com/ChristopherDavenport/semigroups")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := true,
    pomIncludeRepository := { _ =>
      false
    },
    pomExtra := {
      <developers>
        {for ((username, name) <- contributors) yield
        <developer>
          <id>{username}</id>
          <name>{name}</name>
          <url>http://github.com/{username}</url>
        </developer>
        }
      </developers>
    }
  )
}

lazy val mimaSettings = {
  import sbtrelease.Version

  def semverBinCompatVersions(major: Int, minor: Int, patch: Int): Set[(Int, Int, Int)] = {
    val majorVersions: List[Int] = List(major)
    val minorVersions : List[Int] =
      if (major >= 1) Range(0, minor).inclusive.toList
      else List(minor)
    def patchVersions(currentMinVersion: Int): List[Int] =
      if (minor == 0 && patch == 0) List.empty[Int]
      else if (currentMinVersion != minor) List(0)
      else Range(0, patch - 1).inclusive.toList

    val versions = for {
      maj <- majorVersions
      min <- minorVersions
      pat <- patchVersions(min)
    } yield (maj, min, pat)
    versions.toSet
  }

  def mimaVersions(version: String): Set[String] = {
    Version(version) match {
      case Some(Version(major, Seq(minor, patch), _)) =>
        semverBinCompatVersions(major.toInt, minor.toInt, patch.toInt)
          .map{case (maj, min, pat) => maj.toString + "." + min.toString + "." + pat.toString}
      case _ =>
        Set.empty[String]
    }
  }
  // Safety Net For Exclusions
  lazy val excludedVersions: Set[String] = Set()

  // Safety Net for Inclusions
  lazy val extraVersions: Set[String] = Set()

  Seq(
    mimaFailOnProblem := mimaVersions(version.value).toList.headOption.isDefined,
    mimaPreviousArtifacts := (mimaVersions(version.value) ++ extraVersions)
      .filterNot(excludedVersions.contains(_))
      .map{v =>
        val moduleN = moduleName.value + "_" + scalaBinaryVersion.value.toString
        organization.value % moduleN % v
      },
    mimaBinaryIssueFilters ++= {
      import com.typesafe.tools.mima.core._
      import com.typesafe.tools.mima.core.ProblemFilters._
      Seq(
        ProblemFilters.exclude[DirectMissingMethodProblem]("io.chrisdavenport.semigroups.First.firstSemigroup"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("io.chrisdavenport.semigroups.Intersect.IntersectEq"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("io.chrisdavenport.semigroups.Last.lastSemigroup")
      )
    }
  )
}

lazy val skipOnPublishSettings = Seq(
  publish / skip := true,
  publish := (()),
  publishLocal := (()),
  publishArtifact := false,
  publishTo := None
)

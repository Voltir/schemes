import scala.xml.{ Elem, Node, NodeSeq }
import scala.xml.transform.{ RewriteRule, RuleTransformer }

enablePlugins(ScalaJSPlugin)

lazy val schemes = project.in(file("."))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(noPublishSettings)
  .aggregate(coreJS, coreJVM)

lazy val core = crossProject.in(file("core"))
  .enablePlugins(TutPlugin)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    moduleName := "schemes-core",
    scalacOptions.in(Tut) ~= filterConsoleScalacOptions,
    tutTargetDirectory := file(".")
  )

lazy val coreJS = core.js
lazy val coreJVM = core.jvm

lazy val commonSettings = Def.settings(
  organization := "io.github.davidgregory084",

  organizationName := "David Gregory",

  releaseCrossBuild := true,

  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "1.0.0-RC2",
    "org.typelevel" %% "cats-free" % "1.0.0-RC2",
    "org.typelevel" %% "cats-testkit" % "1.0.0-RC2" % Test
  ),

  libraryDependencies ++= {
    scalaBinaryVersion.value match {
      case "2.10" =>
        compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full) :: Nil
      case _ =>
        Nil
    }
  },

  headerCreate.in(Compile) := {
    headerCreate.in(Compile).triggeredBy(compile.in(Compile)).value
  },

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.5" cross CrossVersion.binary),

  headerLicense := Some(HeaderLicense.ALv2("2017", "David Gregory and the Schemes project contributors")),

  unmanagedSources.in(Compile, headerCreate) ++= (sourceDirectory.in(Compile).value / "boilerplate" ** "*.template").get,

  coursierVerbosity := {
    val travisBuild = isTravisBuild.in(Global).value

    if (travisBuild)
      0
    else
      coursierVerbosity.value
  }
)

lazy val publishSettings = Def.settings(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishArtifact.in(Test) := false,
  pomIncludeRepository := Function.const(false),
  autoAPIMappings := true,
  apiURL := Some(url("https://DavidGregory084.github.io/schemes/api/")),

  homepage := Some(url("https://github.com/DavidGregory084/schemes")),

  startYear := Some(2017),

  licenses += ("Apache 2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),

  scmInfo := Some(ScmInfo(
    url("https://github.com/DavidGregory084/schemes"),
    "scm:git:git@github.com:DavidGregory084/schemes.git"
  )),

  developers := List(Developer(
    "DavidGregory084", "David Gregory",
    "davidgregory084@gmail.com",
    url("https://twitter.com/DavidGregory084")
  )),

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },

  credentials ++= (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq,

  pomPostProcess := { (node: Node) =>
    new RuleTransformer(new RewriteRule {
      override def transform(node: Node): NodeSeq = node match {
        case elem: Elem =>
          val isDependency = elem.label == "dependency"
          val isInTestScope = elem.child.exists(c => c.label == "scope" && c.text == "test")

          if (isDependency && isInTestScope)
            Nil
          else
            elem

        case _ =>
          node
      }
    }).transform(node).head
  },

  releaseProcess := {
    import ReleaseTransformations._

    Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      ReleaseStep(
        action = state => state.copy(
          remainingCommands = Exec("sonatypeReleaseAll", None) +: state.remainingCommands
        ),
        enableCrossBuild = true
      ),
      pushChanges
    )
  }
)

lazy val noPublishSettings = Def.settings(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

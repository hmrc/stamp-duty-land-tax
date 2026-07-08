import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "10.7.0"
  private val hmrcMongoVersion = "2.12.0"
  private val pekkoVersion     = "1.1.2"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "io.github.samueleresca"  %% "pekko-quartz-scheduler"     % "1.3.0-pekko-1.1.x",
    "org.apache.santuario"    %  "xmlsec"                     % "3.0.5",
    "commons-codec"           %  "commons-codec"              % "1.17.1"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion            % Test
  )

  val overrides: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-protobuf-v3"           % pekkoVersion,
    "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
    "org.apache.pekko" %% "pekko-stream"                % pekkoVersion
  )

  val it = Seq.empty
}

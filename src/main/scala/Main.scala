import zio._
import zio.config._
import zio.config.typesafe._
import zio.config.magnolia._
import _root_.config.Configuration._
import java.io.IOException
import zio.logging.backend.SLF4J
import zio.logging.{loggerName, consoleJsonLogger}
import zio.logging.LogFormat
import zio.logging.ConsoleLoggerConfig
import services.Cbr


object Gtbot4s extends ZIOAppDefault:

    // val config: ConsoleLoggerConfig = ConsoleLoggerConfig.default.format.toJsonLogger

    override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
        Runtime.removeDefaultLoggers >>> SLF4J.slf4j

    val app: ZIO[AppConfig & Cbr, Throwable, Unit] = for
        _ <- ZIO.logInfo(">>> APPLICATION START <<<") @@ loggerName("Gtbot4s")
        appConfig <- ZIO.service[AppConfig]
        _ <- ZIO.logInfo(s"AppConfig: ${appConfig}") @@ loggerName("Gtbot4s")
        cbr <- ZIO.serviceWithZIO[Cbr](_.fetchAll)
        _ <- ZIO.foreachDiscard(cbr)(rate => ZIO.logInfo(s"Rate: ${rate}"))
    yield ()
    
    override val run = app
        .foldCauseZIO(
            failure => ZIO.logErrorCause(failure), 
            success => ZIO.logInfo(">>> SUCCESS! <<<") @@ loggerName("Gtbot4s")
        )
        .provide(AppConfig.layer, Cbr.live)
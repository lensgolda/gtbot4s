import java.io.IOException

import _root_.config.Configuration.*
import services.Cbr
import zio.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*
import zio.http.*
import zio.logging.ConsoleLoggerConfig
import zio.logging.LogFormat
import zio.logging.backend.SLF4J
import zio.logging.consoleJsonLogger
import zio.logging.loggerName
import services.messaging.TelegramLive
import services.messaging.TelegramService

object Gtbot4s extends ZIOAppDefault:

    override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
        Runtime.removeDefaultLoggers >>> SLF4J.slf4j

    val app = for
        _ <- ZIO.logInfo(">>> APPLICATION START <<<") @@ loggerName("Gtbot4s")
        appConfig <- ZIO.service[AppConfig]
        _ <- ZIO.logInfo(s"AppConfig: ${appConfig}") @@ loggerName("Gtbot4s")
        // ratesList <- ZIO.serviceWithZIO[Cbr](_.fetchAll)
        // _ <- ZIO.foreachDiscard(ratesList)(rate =>
        //     ZIO.logInfo(s"Rate: ${rate}")
        // )
        _ <- ZIO.serviceWithZIO[TelegramService](_.sendMessage(s"Testing"))
    yield ()

    override val run = app
        .foldCauseZIO(
          failure => ZIO.logErrorCause(failure),
          success => ZIO.logInfo(">>> SUCCESS! <<<") @@ loggerName("Gtbot4s")
        )
        .provide(
          AppConfig.layer,
          //   Cbr.live,
          TelegramService.live,
          Client.default
        )

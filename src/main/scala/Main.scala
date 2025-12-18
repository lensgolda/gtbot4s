import java.io.IOException

import _root_.config.Configuration.*
import extensions.CalendarEventFormatter.format
import services.*
import zio.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver
import zio.http.netty.server.NettyDriver
import zio.logging.*
import zio.logging.backend.SLF4J

object Gtbot4s extends ZIOAppDefault:

    override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
        Runtime.removeDefaultLoggers >>> SLF4J.slf4j

    val clientConfig: zio.http.ZClient.Config = ZClient.Config.default
        .connectionTimeout(500.milliseconds)
        .idleTimeout(1500.milliseconds)

    val app
        : ZIO[AppConfig & TelegramService & CalendarService, Throwable, Unit] =
        for
            _ <- ZIO.logInfo(">>> Gtbot4s start <<<") @@ loggerName("Gtbot4s")
            appConfig <- ZIO.service[AppConfig]
            _ <- ZIO.logDebug(s"AppConfig: ${appConfig}") @@ loggerName(
              "Gtbot4s"
            )

            events <- CalendarService.getUpcomingEvents
            message <- ZIO.attempt(events.format)
            telegramService <- ZIO.service[TelegramService]

            idsList = List(
              appConfig.telegram.lensID.value,
              appConfig.telegram.egolkaID.value,
              appConfig.telegram.chatID.value
            )
            _ <- ZIO.foreachParDiscard(idsList)(id =>
                telegramService.sendMessage(id, message)
            )
        yield ()

    override val run: ZIO[Environment & (ZIOAppArgs & Scope), Any, Any] = app
        .foldCauseZIO(
          failure => ZIO.logErrorCause(failure),
          success => ZIO.logInfo(">>> Success! <<<") @@ loggerName("Gtbot4s")
        )
        .provide(
          AppConfig.layer,
          //   Cbr.live,
          CalendarService.live,
          GoogleCalendarService.live,
          TelegramService.live,
          // Zio Client & settings
          Client.customized,
          DnsResolver.default,
          NettyClientDriver.live,
          ZLayer.succeed(NettyConfig.default),
          ZLayer.succeed(clientConfig)
          // Debug
          // ZLayer.Debug.tree
          // Scope.default,
        )

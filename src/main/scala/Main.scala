import java.io.IOException

import _root_.config.Configuration.*
import services.*
import zio.*
import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.client.NettyClientDriver
import zio.http.netty.server.NettyDriver
import zio.logging.ConsoleLoggerConfig
import zio.logging.LogFormat
import zio.logging.backend.SLF4J
import zio.logging.consoleJsonLogger
import zio.logging.loggerName

object Gtbot4s extends ZIOAppDefault:

    override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
        Runtime.removeDefaultLoggers >>> SLF4J.slf4j

    val clientConfig = ZClient.Config.default
        .connectionTimeout(500.milliseconds)
        .idleTimeout(1500.milliseconds)

    val app = for
        _ <- ZIO.logInfo(">>> Gtbot4s start <<<") @@ loggerName("Gtbot4s")
        appConfig <- ZIO.service[AppConfig]
        idsList = List(
          // appConfig.telegram.lensID.value,
          // appConfig.telegram.egolkaID.value,
          appConfig.telegram.chatID.value
        )
        _ <- ZIO.logInfo(s"AppConfig: ${appConfig}") @@ loggerName("Gtbot4s")
        // ratesList <- ZIO.serviceWithZIO[Cbr](_.fetchAll)
        // calendars <- ZIO.serviceWithZIO[CalendarService](_.getCalendarList())
        calendarService <- ZIO.service[CalendarService]
        telegramService <- ZIO.service[TelegramService]
        events <- calendarService.getUpcomingEvents(3)
        message <- ZIO.attempt(CalendarEventFormatter.formatEventsList(events))

        _ <- ZIO.foreachParDiscard(idsList)(id =>
            telegramService.sendMessage(id, message)
        )
    yield ()

    override val run = app
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
          // Scope.default,
        )

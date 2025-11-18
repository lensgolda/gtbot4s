import java.io.IOException

import _root_.config.Configuration.*
// import services.Cbr
// import services.TelegramLive
// import services.TelegramService
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
import services._

object Gtbot4s extends ZIOAppDefault:

    override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
        Runtime.removeDefaultLoggers >>> SLF4J.slf4j

    val clientConfig = ZClient.Config.default
        .connectionTimeout(500.milliseconds)
        .idleTimeout(1500.milliseconds)

    val app = for
        _ <- ZIO.logInfo(">>> APPLICATION START <<<") @@ loggerName("Gtbot4s")
        appConfig <- ZIO.service[AppConfig]
        _ <- ZIO.logInfo(s"AppConfig: ${appConfig}") @@ loggerName("Gtbot4s")
        // ratesList <- ZIO.serviceWithZIO[Cbr](_.fetchAll)
        calendars <- ZIO.serviceWithZIO[CalendarService](_.getCalendarList())

        events <- ZIO.serviceWithZIO[CalendarService](_.getUpcomingEvents(10))
        _ <- ZIO.foreachDiscard(calendars)(calendar =>
            ZIO.logInfo(
              s"Calendar length: ${calendars.length}, ${calendar.id}, ${calendar.summary}, ${calendar.description}, ${calendar.primary}"
            )
        )
        _ <- ZIO.foreachDiscard(events)(event =>
            Console.printLine(event)
            ZIO.logInfo(s"Event: ${event.summary} at ${event.start.dateTime}")
        )
    // _ <- ZIO.serviceWithZIO[TelegramService](_.sendMessage("Testing"))
    // result <- ZIO.collectAllPar(List(effect1, effect2))
    yield ()

    override val run = app
        .foldCauseZIO(
          failure => ZIO.logErrorCause(failure),
          success => ZIO.logInfo(">>> SUCCESS! <<<") @@ loggerName("Gtbot4s")
        )
        .provide(
          AppConfig.layer,
          //   Cbr.live,
          CalendarService.live,
          //   TelegramService.live,
          GoogleCalendarService.live
          // Zio Client & settings
          //   Client.customized,
          //   DnsResolver.default,
          //   NettyClientDriver.live,
          //   ZLayer.succeed(NettyConfig.default),
          //   ZLayer.succeed(clientConfig)
        )

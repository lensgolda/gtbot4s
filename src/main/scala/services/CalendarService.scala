package services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

import config.Configuration.AppConfig
import domain.*
import domain.calendar.*
import zio.*

trait CalendarService:
    def getUpcomingEvents: IO[CalendarError, CalendarEvents]
    def getCalendarList: IO[CalendarError, GoogleCalendars]

final class CalendarServiceLive(
    appConfig: AppConfig,
    googleCalendarService: GoogleCalendarService
) extends CalendarService:
    override def getUpcomingEvents: IO[CalendarError, CalendarEvents] =
        for
            now <- ZIO.succeed(
              LocalDate.now.atStartOfDay(ZoneId.of("Europe/Kaliningrad"))
            )
            events <- googleCalendarService.listEvents(
              maxResults = 15,
              timeMin = Some(now),
              timeMax = Some(now.plusDays(appConfig.googleCalendar.daysRange))
            )
        yield events

    override def getCalendarList: IO[CalendarError, GoogleCalendars] =
        googleCalendarService.listCalendars

object CalendarService:
    def getUpcomingEvents: ZIO[CalendarService, CalendarError, CalendarEvents] =
        ZIO.serviceWithZIO[CalendarService](_.getUpcomingEvents)

    def getCalendarList: ZIO[CalendarService, CalendarError, GoogleCalendars] =
        ZIO.serviceWithZIO[CalendarService](_.getCalendarList)

    val live: URLayer[GoogleCalendarService with AppConfig, CalendarService] =
        ZLayer.fromFunction(
          (appConfig: AppConfig, calendarService: GoogleCalendarService) =>
              new CalendarServiceLive(appConfig, calendarService)
        )

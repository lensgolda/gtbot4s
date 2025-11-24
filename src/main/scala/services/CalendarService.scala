package services

import java.time.ZoneId
import java.time.ZonedDateTime

import config.Configuration.AppConfig
import domain.*
import zio.ZIO
import zio.ZLayer
import java.time.LocalDateTime
import java.time.LocalDate

trait CalendarService:
    def getUpcomingEvents(
        days: Int
    ): ZIO[Any, CalendarError, List[CalendarEvent]]

    def getCalendarList(): ZIO[Any, CalendarError, List[GoogleCalendar]]

final class CalendarServiceLive(googleCalendarService: GoogleCalendarService)
    extends CalendarService:
    override def getUpcomingEvents(
        days: Int
    ): ZIO[Any, CalendarError, List[CalendarEvent]] =
        for
            now <- ZIO.succeed(
              LocalDate.now.atStartOfDay(ZoneId.of("Europe/Kaliningrad"))
            )
            events <- googleCalendarService.listEvents(
              maxResults = 15,
              timeMin = Some(now),
              timeMax = Some(now.plusDays(days))
            )
        yield events

    override def getCalendarList()
        : ZIO[Any, CalendarError, List[GoogleCalendar]] =
        for calendars <- googleCalendarService.listCalendars()
        yield calendars

object CalendarService:
    private val calendarServiceZIO =
        for googleCalendarService <- ZIO.service[GoogleCalendarService]
        yield new CalendarServiceLive(googleCalendarService)

    val live: ZLayer[GoogleCalendarService, Nothing, CalendarService] =
        ZLayer.fromZIO(calendarServiceZIO)

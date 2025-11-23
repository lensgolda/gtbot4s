package services

import java.time.ZoneId
import java.time.ZonedDateTime

import config.Configuration.AppConfig
import domain.*
import zio.ZIO
import zio.ZLayer

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
              ZonedDateTime.now(ZoneId.of("Europe/Kaliningrad"))
            )
            plus3Days = now.plusDays(days)
            events <- googleCalendarService.listEvents(
              maxResults = 10,
              timeMin = Some(now),
              timeMax = Some(plus3Days)
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

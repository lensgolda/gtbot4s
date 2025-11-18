package services

import domain.CalendarEvent
import zio.ZIO
import domain.CalendarError
import java.time.ZonedDateTime
import zio.ZLayer
import config.Configuration.AppConfig
import com.google.api.services.calendar.model.CalendarListEntry
import domain.GoogleCalendar

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
            now <- ZIO.succeed(ZonedDateTime.now())
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

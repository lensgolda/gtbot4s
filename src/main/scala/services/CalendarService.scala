package services

import domain.CalendarEvent
import zio.ZIO
import domain.CalendarError
import java.time.ZonedDateTime
import zio.ZLayer
import config.Configuration.AppConfig

trait CalendarService:
    def getUpcomingEvents(
        days: Int
    ): ZIO[Any, CalendarError, List[CalendarEvent]]

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

object CalendarService:
    private val calendarServiceZIO =
        for googleCalendarService <- ZIO.service[GoogleCalendarService]
        yield new CalendarServiceLive(googleCalendarService)

    val live: ZLayer[GoogleCalendarService, Nothing, CalendarService] =
        ZLayer.fromZIO(calendarServiceZIO)

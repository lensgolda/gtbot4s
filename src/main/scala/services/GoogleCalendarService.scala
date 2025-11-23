package services

import java.io.FileInputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections

import _root_.config.Configuration.AppConfig
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.CalendarListEntry
import com.google.api.services.calendar.model.Event
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import domain.*
import zio.*
import zio.http.*

import scala.jdk.CollectionConverters.ListHasAsScala

trait GoogleCalendarService:
    def listEvents(
        calendarID: Option[String] = None,
        maxResults: Int = 50,
        timeMin: Option[ZonedDateTime] = None,
        timeMax: Option[ZonedDateTime] = None
    ): ZIO[Any, CalendarError, List[CalendarEvent]]
    def listCalendars(): ZIO[Any, CalendarError, List[GoogleCalendar]]

final class GoogleCalendarLive(config: AppConfig, calendar: Calendar)
    extends GoogleCalendarService:

    // RFC3339
    private val formatterRFC3339 =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    override def listEvents(
        calendarID: Option[String],
        maxResults: Int,
        timeMin: Option[ZonedDateTime],
        timeMax: Option[ZonedDateTime]
    ): ZIO[Any, CalendarError, List[CalendarEvent]] =
        ZIO.attempt:
            val cID = calendarID.getOrElse(config.googleCalendar.targetCalendar)
            val req = calendar.events
                .list(cID)
                .setMaxResults(maxResults)
                .setSingleEvents(true)
                .setOrderBy("startTime")
                .setPrettyPrint(true)
            timeMin.foreach(tmin =>
                req.setTimeMin(DateTime(tmin.format(formatterRFC3339)))
            )

            timeMax.foreach(tmax =>
                req.setTimeMax(DateTime(tmax.format(formatterRFC3339)))
            )

            val result = req.execute
            Option
                .apply(result.getItems)
                .map(_.asScala.toList)
                .getOrElse(List.empty[Event])
                .map(convertGoogleEvent)
        .tapError(e => ZIO.logError(s"GoogleError: ${e.getMessage}"))
            .mapError:
                case e: GoogleJsonResponseException if e.getStatusCode == 404 =>
                    CalendarError.NotFoundError(
                      s"Calendar not found"
                    )
                case e: Throwable =>
                    CalendarError.ServiceError(
                      s"Failed to list events: ${e.getCause}"
                    )

    private def convertGoogleEvent(event: Event): CalendarEvent =
        val start = event.getStart
        val end = event.getEnd

        CalendarEvent(
          event.getId,
          Option(event.getSummary)
              .getOrElse(String("Название встречи отсутствует")),
          Option(event.getDescription),
          EventDateTime(
            date = Option(start.getDate).map(_.toString),
            dateTime = Option(start.getDateTime).map(dt =>
                ZonedDateTime.parse(dt.toStringRfc3339)
            ),
            timeZone = Option(start.getTimeZone)
          ),
          EventDateTime(
            date = Option(end.getDate).map(_.toString),
            dateTime = Option(end.getDateTime).map(dt =>
                ZonedDateTime.parse(dt.toStringRfc3339)
            ),
            timeZone = Option(end.getTimeZone)
          ),
          location = Option(event.getLocation),
          status = event.getStatus,
          created = ZonedDateTime.parse(event.getCreated.toStringRfc3339),
          updated = ZonedDateTime.parse(event.getUpdated.toStringRfc3339)
        )

    private def convertCalendarListEntry(
        calendarListEntry: CalendarListEntry
    ): GoogleCalendar =
        GoogleCalendar(
          calendarListEntry.getId,
          calendarListEntry.getSummary,
          calendarListEntry.getDescription,
          calendarListEntry.getPrimary
        )
    override def listCalendars()
        : ZIO[Any, CalendarError, List[GoogleCalendar]] =
        ZIO.attempt:
            val res = calendar.calendarList.list.execute
            Option(res.getItems.asScala.toList)
                .getOrElse(List.empty[CalendarListEntry])
                .map(convertCalendarListEntry)
        .tapError(e => ZIO.logError(s"GoogleError: ${e.getMessage}"))
            .mapError:
                case e: GoogleJsonResponseException if e.getStatusCode == 404 =>
                    CalendarError.NotFoundError(
                      s"Calendar not found"
                    )
                case e: Throwable =>
                    CalendarError.ServiceError(
                      s"Failed to list calendars: ${e.getMessage}"
                    )

object GoogleCalendarService:
    val live: ZLayer[AppConfig, CalendarError, GoogleCalendarService] =
        ZLayer.scoped(
          for
              config <- ZIO.service[AppConfig]
              calendar <- makeCalendar(config)
          yield new GoogleCalendarLive(config, calendar)
        )

    private def makeCalendar(
        config: AppConfig
    ): ZIO[Scope, CalendarError, Calendar] =
        ZIO.attempt:
            val credentials = ServiceAccountCredentials
                .fromStream(
                  new FileInputStream(config.googleCalendar.keyFile)
                )
                .createScoped(
                  Collections.singletonList(CalendarScopes.CALENDAR)
                )
            new Calendar.Builder(
              GoogleNetHttpTransport.newTrustedTransport,
              GsonFactory.getDefaultInstance,
              new HttpCredentialsAdapter(credentials)
            ).build
        .mapError(e =>
                CalendarError.AuthenticationError(
                  s"Failed to make Calendar instance: ${e.getMessage}"
                )
            )

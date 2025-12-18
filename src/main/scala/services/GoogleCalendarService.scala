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
import domain.calendar.*
import extensions.GoogleCalendarConverter.*
import zio.*
import zio.http.*

import scala.jdk.CollectionConverters.ListHasAsScala

trait GoogleCalendarService:
    def listEvents(
        calendarID: Option[String] = None,
        maxResults: Int = 50,
        timeMin: Option[ZonedDateTime] = None,
        timeMax: Option[ZonedDateTime] = None
    ): IO[CalendarError, CalendarEvents]
    def listCalendars: IO[CalendarError, GoogleCalendars]

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
    ): IO[CalendarError, CalendarEvents] =
        ZIO.attemptBlocking:
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

    override def listCalendars: IO[CalendarError, GoogleCalendars] =
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

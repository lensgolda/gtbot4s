package services

import zio.*
import zio.http.*
import zio.schema.*
import zio.schema.annotation.fieldName
// import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
// import zio.json.JsonCodec
import java.time.ZonedDateTime
import java.io.FileInputStream
import java.time.format.DateTimeFormatter
import java.util.Collections

import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.auth.http.HttpCredentialsAdapter

import scala.jdk.CollectionConverters.ListHasAsScala
// import domain.CalendarEvent
// import domain.EventDateTime
import _root_.config.Configuration.AppConfig
import domain._

trait GoogleCalendarService:
    def listEvents(
        calendarID: Option[String] = None,
        maxResults: Int = 50,
        timeMin: Option[ZonedDateTime] = None,
        timeMax: Option[ZonedDateTime] = None
    ): ZIO[Any, CalendarError, List[CalendarEvent]]

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
            val req = calendar
                .events()
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

            val result = req.execute()
            Option
                .apply(result.getItems())
                .map(_.asScala.toList)
                .getOrElse(List.empty[Event])
                .map(convertGoogleEvent)
        .tapError(e =>
                ZIO.logError(s"GoogleError: ${}, message: ${e.getMessage}")
            )
            .mapError:
                case e: GoogleJsonResponseException if e.getStatusCode == 404 =>
                    CalendarError.NotFoundError(
                      s"Calendar not found"
                    )
                case e: GoogleJsonResponseException if e.getStatusCode == 403 =>
                    CalendarError.ServiceError(
                      s"Google API error: ${e.getStatusCode}, ${e.getDetails}"
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
                ZonedDateTime.parse(dt.toStringRfc3339())
            ),
            timeZone = Option(start.getTimeZone)
          ),
          EventDateTime(
            date = Option(end.getDate).map(_.toString),
            dateTime = Option(end.getDateTime).map(dt =>
                ZonedDateTime.parse(dt.toStringRfc3339())
            ),
            timeZone = Option(end.getTimeZone)
          ),
          location = Option(event.getLocation),
          status = event.getStatus,
          created = ZonedDateTime.parse(event.getCreated.toStringRfc3339()),
          updated = ZonedDateTime.parse(event.getUpdated.toStringRfc3339())
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
                  FileInputStream(config.googleCalendar.keyFile)
                )
                .createScoped(
                  Collections.singletonList(CalendarScopes.CALENDAR)
                )
            new Calendar.Builder(
              GoogleNetHttpTransport.newTrustedTransport(),
              GsonFactory.getDefaultInstance(),
              new HttpCredentialsAdapter(credentials)
            ).build()
        .mapError(e =>
                CalendarError.AuthenticationError(
                  s"Failed to create GoogleCalendarLive service: ${e.getMessage}"
                )
            )

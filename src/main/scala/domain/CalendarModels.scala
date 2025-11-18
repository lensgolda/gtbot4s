package domain

import java.time.ZonedDateTime
import zio.json.JsonCodec

sealed trait CalendarError extends Exception

object CalendarError:
    case class AuthenticationError(message: String) extends CalendarError
    case class NotFoundError(message: String) extends CalendarError
    case class ValidationError(message: String) extends CalendarError
    case class ServiceError(message: String) extends CalendarError

case class CalendarEvent(
    id: String,
    summary: String,
    description: Option[String],
    start: EventDateTime,
    end: EventDateTime,
    location: Option[String],
    status: String,
    created: ZonedDateTime,
    updated: ZonedDateTime
) derives JsonCodec

case class GoogleCalendar(
    id: String,
    summary: String,
    description: String,
    primary: Boolean
) derives JsonCodec

case class EventDateTime(
    date: Option[String],
    dateTime: Option[ZonedDateTime],
    timeZone: Option[String]
) derives JsonCodec

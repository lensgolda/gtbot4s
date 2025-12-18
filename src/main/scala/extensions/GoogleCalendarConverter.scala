package extensions

import java.time.ZonedDateTime

import com.google.api.services.calendar.model.CalendarListEntry
import com.google.api.services.calendar.model.Event
import domain.calendar.*

object GoogleCalendarConverter:
    extension (calendarListEntry: CalendarListEntry)
        def convertCalendarListEntry: GoogleCalendar =
            GoogleCalendar(
              calendarListEntry.getId,
              calendarListEntry.getSummary,
              calendarListEntry.getDescription,
              calendarListEntry.getPrimary
            )

    extension (event: Event)
        def convertGoogleEvent: CalendarEvent =
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

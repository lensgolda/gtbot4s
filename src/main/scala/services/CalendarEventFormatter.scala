package services

import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import domain.CalendarEvent
import java.util.Locale

object CalendarEventFormatter:
    private val dayOfWeekFormatter =
        DateTimeFormatter.ofPattern("EEE", Locale.of("ru", "RU"))
    private val time24hFormatter = DateTimeFormatter.ofPattern("HH:mm")

    def formatEvent(event: CalendarEvent): String =
        event.start.dateTime match
            case Some(dateTime) =>
                val dayOfWeek = dateTime.format(dayOfWeekFormatter)
                val timeStart = dateTime.format(time24hFormatter)
                val endTime = event.end.dateTime
                    .map(_.format(time24hFormatter))
                    .getOrElse("")
                s"🗓️ $dayOfWeek $timeStart - $endTime | 📍 ${event.summary}"

            case None =>
                // All-day event or date-only
                event.start.date
                    .map(dateStr =>
                        val date = java.time.LocalDate.parse(dateStr)
                        val dayOfWeek = date.format(dayOfWeekFormatter)
                        s"🔔 $dayOfWeek | 📍 ${event.summary}"
                    )
                    .getOrElse(event.summary)

    def formatEventsList(events: List[CalendarEvent]) =
        events.map(event => formatEvent(event)).mkString("\n")

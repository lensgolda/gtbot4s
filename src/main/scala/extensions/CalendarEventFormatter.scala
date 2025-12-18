package extensions

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

import domain.calendar.*

object CalendarEventFormatter:
    private val dayOfWeekFormatter =
        DateTimeFormatter.ofPattern("EEE", Locale.of("ru", "RU"))
    private val time24hFormatter = DateTimeFormatter.ofPattern("HH:mm")

    extension (event: CalendarEvent)
        def format: String =
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

    extension (events: CalendarEvents)
        def format: String =
            events.map(_.format).mkString("\n")

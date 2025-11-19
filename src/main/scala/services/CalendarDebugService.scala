package services

import zio._
import zio.logging._
import com.google.api.services.calendar.Calendar
import scala.jdk.CollectionConverters._

object CalendarDebugService {

    def debugCalendarAccess(calendar: Calendar): ZIO[Any, Throwable, Unit] =
        for {
            // 1. Test listing all accessible calendars
            _ <- ZIO.logInfo("=== Debugging Calendar Access ===")

            // Get calendar list to see what's accessible
            calendarList <- ZIO.attempt {
                calendar.calendarList().list().execute()
            }

            _ <- ZIO.logInfo(
              s"Total calendars accessible: ${Option(calendarList.getItems).map(_.size()).getOrElse(0)}"
            )

            // Log each calendar details
            _ <- ZIO.foreachDiscard(
              Option(calendarList.getItems)
                  .getOrElse(java.util.Collections.emptyList())
                  .asScala
            ) { cal =>
                ZIO.logInfo(
                  s"Calendar: ${cal.getSummary} (ID: ${cal.getId}, Access: ${cal.getAccessRole})"
                )
            }

            // 2. Test with specific calendar IDs
            testCalendarIds = List(
              "primary",
              "aleksey.golda@flocktory.com",
              "g4calendar-sa@calendarapi-478522.iam.gserviceaccount.com"
            ) // Replace with actual email

            _ <- ZIO.foreachDiscard(testCalendarIds) { calId =>
                for {
                    _ <- ZIO.logInfo(s"Testing calendar: $calId")
                    events <- ZIO
                        .attempt {
                            calendar
                                .events()
                                .list(calId)
                                .setMaxResults(5)
                                .execute()
                        }
                        .catchAll { e =>
                            ZIO.logError(
                              s"Failed to access calendar $calId: ${e.getMessage}"
                            ) *>
                                ZIO.succeed(null)
                        }

                    _ <-
                        if (events != null) {
                            ZIO.logInfo(
                              s"Calendar $calId - Events found: ${Option(
                                    events.getItems
                                  ).map(_.size()).getOrElse(0)}"
                            )
                        } else ZIO.unit

                } yield ()
            }

        } yield ()
}

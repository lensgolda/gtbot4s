package extensions

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object CbrRatesDateParser:
    private val df = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    extension (date: String) def parse: LocalDate = LocalDate.parse(date, df)

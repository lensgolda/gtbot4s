package domain

import java.time.LocalDate

case class CbrRate(
    numCode: String,
    charCode: String,
    nominal: String,
    name: String,
    value: String,
    date: LocalDate
)

package domain.rates

import java.time.LocalDate

type Rates = Seq[CbrRate]

case class CbrRate(
    numCode: String,
    charCode: String,
    nominal: String,
    name: String,
    value: String,
    date: LocalDate
)

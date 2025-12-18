package givens

import domain.rates.*
import extensions.CbrRatesDateParser.*
import zio.*

import scala.xml.*

trait XmlParser[A, B]:
    def parse(xmlDoc: A): B

object XmlParser:
    given XmlParser[Elem, Task[Rates]] with
        def parse(xmlDoc: Elem): Task[Rates] = for
            date <- ZIO.attempt(xmlDoc \@ "Date")
            ld <- ZIO.attempt(date.parse)
            _ <- ZIO.logInfo(s"Rates for date: $ld")
            cbrRates <- ZIO.attempt(
              for
                  valute <- xmlDoc \ "Valute"
                  vnumCode = valute \ "NumCode"
                  vcharCode = valute \ "CharCode"
                  vnominal = valute \ "Nominal"
                  vname = valute \ "Name"
                  vvalue = valute \ "Value"
              yield new CbrRate(
                vnumCode.text,
                vcharCode.text,
                vnominal.text,
                vname.text,
                vvalue.text,
                ld
              )
            )
        yield cbrRates

object CbrRateXmlParser:
    def parseRates(xmlDoc: Elem)(using
        xmlParser: XmlParser[Elem, Task[Rates]]
    ): Task[Rates] = xmlParser.parse(xmlDoc)

package services

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import _root_.config.Configuration.*
import domain.CbrRate
import zio.*
import zio.http.*
import zio.schema.*

import scala.xml.*

import XmlCodec.*

trait Cbr {
    def fetchAll: ZIO[Any, Throwable, List[CbrRate]]
}

final class CbrLive(config: AppConfig, httpClient: Client) extends Cbr:
    private val client = httpClient.batched

    def parseDate(date: String): ZIO[Any, Throwable, LocalDate] =
        ZIO.attempt:
            val df = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            LocalDate.parse(date, df)

    private def parseRates(xmlDoc: Elem): ZIO[Any, Throwable, Seq[CbrRate]] =
        for
            date <- ZIO.attempt(xmlDoc \@ "Date")
            ld <- parseDate(date)
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

    private val sendRatesReq: ZIO[Any, Throwable, Seq[CbrRate]] = for
        url <- ZIO.fromEither(URL.decode(config.cbr.baseURL.toString))
        res <- client.request(Request.get(url))
        xml <- res.body.asXmlZIO
        cbrRates <- parseRates(xml)
    yield cbrRates

    override def fetchAll: ZIO[Any, Throwable, List[CbrRate]] =
        for rates <- sendRatesReq
        yield rates.toList

object Cbr:
    val live: ZLayer[AppConfig & Client, Throwable, Cbr] = ZLayer(
      for
          appConfig <- ZIO.service[AppConfig]
          httpClient <- ZIO.service[Client]
      yield new CbrLive(appConfig, httpClient)
    )

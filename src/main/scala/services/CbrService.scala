package services

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import _root_.config.Configuration.*
import domain.rates.*
import extensions.CbrRatesDateParser.*
import extensions.XmlCodec.*
import givens.CbrRateXmlParser
import zio.*
import zio.http.*
import zio.schema.*

import scala.xml.*

trait Cbr:
    def fetchAll: Task[Rates]

final class CbrLive(config: AppConfig, httpClient: Client) extends Cbr:
    private val client = httpClient.batched

    private val sendRatesReq: Task[Rates] = for
        url <- ZIO.fromEither(URL.decode(config.cbr.baseURL.toString))
        res <- client.request(Request.get(url))
        xml <- res.body.asXmlZIO
        cbrRates <- CbrRateXmlParser.parseRates(xml)
    yield cbrRates

    override def fetchAll: Task[Rates] =
        for rates <- sendRatesReq
        yield rates.toList

object Cbr:
    val live: RLayer[AppConfig & Client, Cbr] = ZLayer.fromZIO(
      for
          appConfig <- ZIO.service[AppConfig]
          httpClient <- ZIO.service[Client]
      yield new CbrLive(appConfig, httpClient)
    )

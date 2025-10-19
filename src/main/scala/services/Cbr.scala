package services

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import _root_.config.Configuration.*
import zio.*
import zio.http.*
import zio.schema.*

import scala.xml.*

case class CbrRate(
    numCode: String,
    charCode: String,
    nominal: String,
    name: String,
    value: String,
    date: LocalDate
)

// object CbrRate:
//     given Schema[CbrRate] = DeriveSchema.gen[CbrRate]
//     given Schema[List[CbrRate]] = Schema.list[CbrRate]
// given Schema[CbrRate] = Schema.CaseClass5[String, String, String, String, String, CbrRate](
//   id0 = TypeId.fromTypeName("CbrRate"),
//   field01 =
//     Schema.Field(
//       name0 = "name",
//       schema0 = Schema[String],
//       get0 = _.name,
//       set0 = (r, x) => r.copy(name = x)
//     ),
//   field02 =
//     Schema.Field(
//       name0 = "charCode",
//       schema0 = Schema[String],
//       get0 = _.charCode,
//       set0 = (r, charCode) => r.copy(charCode = charCode)
//     ),
//     field03 =
//     Schema.Field(
//       name0 = "numCode",
//       schema0 = Schema[String],
//       get0 = _.numCode,
//       set0 = (r, numCode) => r.copy(numCode = numCode)
//     ),
//     field04 =
//     Schema.Field(
//       name0 = "nominal",
//       schema0 = Schema[String],
//       get0 = _.nominal,
//       set0 = (r, nominal) => r.copy(nominal = nominal)
//     ),
//     field05 =
//     Schema.Field(
//       name0 = "value",
//       schema0 = Schema[String],
//       get0 = _.value,
//       set0 = (r, value) => r.copy(value = value)
//     ),
//   construct0 = (name, charCode, numCode, nominal, value) => CbrRate(name, charCode, numCode, nominal, value),
// )

trait Cbr {
    def fetchAll: ZIO[Any, Throwable, List[CbrRate]]
}

final class CbrLive(config: AppConfig, httpClient: Client) extends Cbr:
    private val client = httpClient.batched

    import CbrRate._

//   def parseDate(date: String) =
// 	val df = DateTimeFormatter.ofPattern("dd.MM.yyyy")
//     val ld = LocalDate.parse(date, df)
//     ld
    def parseDate(date: String): ZIO[Any, Throwable, LocalDate] =
        ZIO.attempt:
            val df = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            LocalDate.parse(date, df)

    def parseRates(xmlDoc: Elem): ZIO[Any, Throwable, Seq[CbrRate]] =
        for
            date <- ZIO.attempt(xmlDoc \@ "Date")
            ld <- parseDate(date)
            _ <- ZIO.logInfo(s"Rates for date: $ld")
            cbrRate <- ZIO.attempt(
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
        yield cbrRate

    private def parseXmlBody(
        data: Array[Byte]
    ): ZIO[Any, Throwable, Seq[CbrRate]] =
        for
            bis <- ZIO.attempt(
              BufferedInputStream(ByteArrayInputStream(data))
            )
            xmlDoc <- ZIO.attempt(XML.load(bis))
            parsedRates <- parseRates(xmlDoc)
        yield parsedRates

    private val sendRatesReq: ZIO[Any, Throwable, Seq[CbrRate]] = for
        url <- ZIO.fromEither(URL.decode(config.cbr.baseURL.toString()))
        res <- client.request(Request.get(url))
        rate <- res.body.asArray
        cbrRates <- parseXmlBody(rate)
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

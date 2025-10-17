package services

import java.nio.charset.Charset

import _root_.config.Configuration.*
import zio.*
import zio.http.*
import zio.schema.*


case class CbrRate(
    numCode: String,
    charCode: String,
    nominal: String,
    name: String,
    value: String,
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

    private val sendRatesReq = for
        url <- ZIO.fromEither(URL.decode(config.cbr.baseURL.toString()))
        res <- client.request(Request.get(url))
        rate <- res.body.asString
        _ <- ZIO.logDebug(
                s"${rate}"
            )
    yield ()

    override def fetchAll: ZIO[Any, Throwable, List[CbrRate]] = 
        sendRatesReq *> ZIO.succeed(
            List(
                new CbrRate("156", "CNY", "1", "Юань", "11,0216")
            )
        )

object Cbr:
    val live: ZLayer[AppConfig & Client, Throwable, Cbr] = ZLayer(
        for 
            appConfig <- ZIO.service[AppConfig]
            httpClient <- ZIO.service[Client]
        yield new CbrLive(appConfig, httpClient)
    )
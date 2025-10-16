package services

import zio.ZIO
import scala.util.FromDigits.Decimal
import zio.ZLayer
import config.Configuration.AppConfig
import config.Configuration.CbrConfig

case class CbrRate(
    numCode: String,
    charCOde: String,
    nominal: String,
    name: String,
    value: String,
)

trait Cbr {
  def fetchAll: ZIO[Any, Throwable, List[CbrRate]]
}

final class CbrLive(config: CbrConfig) extends Cbr:
    override def fetchAll: ZIO[Any, Throwable, List[CbrRate]] = ZIO.succeed(
        List(
            new CbrRate("156", "CNY", "1", "Юань", "11,0216")
        )
    )

object Cbr:
    val live: ZLayer[AppConfig, Throwable, Cbr] = ZLayer(
        for appConfig <- ZIO.service[AppConfig]
        yield new CbrLive(appConfig.cbr)
    )
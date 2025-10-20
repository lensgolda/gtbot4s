package services.messaging

import _root_.config.Configuration.*
import zio.*
import zio.http.*
import zio.schema.*
import zio.schema.annotation.fieldName
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

trait TelegramService {
    def sendMessage(message: String): ZIO[Any, Throwable, Unit]
}

final case class SendMessageRequest(
    text: String,
    @fieldName("chat_id") chatID: Long
)

object SendMessageRequest:
    given Schema[SendMessageRequest] =
        DeriveSchema.gen[SendMessageRequest]

final class TelegramLive(config: AppConfig, httpClient: Client)
    extends TelegramService:

    import SendMessageRequest._

    private val message = s"Test message"
    private val telegramUserID = config.telegram.lensID.value
    private val headers = Headers(
      Header.ContentType(MediaType.application.json),
      Header.Accept(MediaType.application.json)
    )

    override def sendMessage(message: String): ZIO[Any, Throwable, Unit] =
        for
            baseUrl <- ZIO.fromEither(
              URL.decode(config.telegram.baseURL.toString)
            )
            resp <- httpClient.batched
                .url(baseUrl)
                .addHeaders(headers)
                .post(s"bot${config.telegram.token}/sendMessage")(
                  Body.from[SendMessageRequest](
                    SendMessageRequest(message, telegramUserID)
                  )
                )
            _ <- ZIO.logInfo(s"Response Status: ${resp.status}")
            _ <- ZIO.logInfo(s"Response Body: ${resp.body.asString}")
        yield ()

object TelegramService:
    val live: ZLayer[AppConfig & Client, Throwable, TelegramService] =
        ZLayer(
          for
              appConfig <- ZIO.service[AppConfig]
              client <- ZIO.service[Client]
          yield new TelegramLive(appConfig, client)
        )

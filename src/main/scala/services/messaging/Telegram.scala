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

    private val makeSendRequest: ZIO[Any, Throwable, Request] =
        ZIO.attempt(
          Request
              .post(
                s"bot${config.telegram.token}/sendMessage",
                Body.from[SendMessageRequest](
                  SendMessageRequest(message, telegramUserID)
                )
              )
              .addHeaders(
                Headers(
                  Header.ContentType(MediaType.application.json),
                  Header.Accept(MediaType.application.json)
                )
              )
        )

    override def sendMessage(message: String): ZIO[Any, Throwable, Unit] =
        for
            url <- ZIO.fromEither(URL.decode(config.telegram.baseURL.toString))
            req <- makeSendRequest
            resp <- httpClient
                .url(url)
                .batched
                .request(req)
            _ <- ZIO.debug(s"Response Status: ${resp.status}")
            _ <- ZIO.debug(s"Response Body: ${resp.body.asString}")
        yield ()

object TelegramService:
    val live: ZLayer[AppConfig & Client, Throwable, TelegramService] =
        ZLayer(
          for
              appConfig <- ZIO.service[AppConfig]
              client <- ZIO.service[Client]
          yield new TelegramLive(appConfig, client)
        )

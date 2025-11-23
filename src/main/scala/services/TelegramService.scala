package services

import _root_.config.Configuration.*
import dto.*
import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

trait TelegramService:
    def sendMessage(
        chatID: Long,
        message: String
    ): ZIO[Any, Throwable, Unit]

final class TelegramLive(config: AppConfig, httpClient: Client)
    extends TelegramService:

    private val message = s"Test message"
    private val telegramUserID = config.telegram.lensID.value
    private val headers = Headers(
      Header.ContentType(MediaType.application.json),
      Header.Accept(MediaType.application.json)
    )

    override def sendMessage(
        chatID: Long,
        message: String
    ): ZIO[Any, Throwable, Unit] =
        for
            baseUrl <- ZIO.fromEither(
              URL.decode(config.telegram.baseURL.toString)
            )
            resp <- httpClient.batched
                .url(baseUrl)
                .addHeaders(headers)
                .post(s"bot${config.telegram.token}/sendMessage")(
                  Body.from[SendMessageRequest](
                    SendMessageRequest(message, chatID)
                  )
                )
            _ <- ZIO.logInfo(s"Response Status: ${resp.status}")
            _ <- ZIO.logInfo(s"Response Body: ${resp.body.asString}")
        yield ()

object TelegramService:
    private val telegramServiceZIO = for
        appConfig <- ZIO.service[AppConfig]
        client <- ZIO.service[Client]
    yield new TelegramLive(appConfig, client)

    val live: ZLayer[AppConfig & Client, Throwable, TelegramService] =
        ZLayer.fromZIO(telegramServiceZIO)

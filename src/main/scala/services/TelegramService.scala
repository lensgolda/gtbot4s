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
    ): Task[Unit]

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
    ): Task[Unit] =
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
            _ <- ZIO.logDebug(s"Response Status: ${resp.status}")
            _ <- ZIO.logDebug(s"Response Body: ${resp.body.asString}")
        yield ()

object TelegramService:
    val live: RLayer[AppConfig & Client, TelegramService] =
        ZLayer.fromFunction((appConfig: AppConfig, client: Client) =>
            new TelegramLive(appConfig, client)
        )

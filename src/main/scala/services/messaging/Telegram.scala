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

sealed trait Peer
object Peer:
    final case class PeerChat(chatID: Long) extends Peer
    final case class PeerUser(userID: Long) extends Peer

    given Schema[Peer] = DeriveSchema.gen[Peer]

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

    private def makeSendRequest(message: String): ZIO[Any, Throwable, Request] =
        ZIO.attempt(
          Request
              .post(
                s"bot${config.telegram.token}/sendMessage",
                Body.from[SendMessageRequest](
                  SendMessageRequest(
                    message,
                    Peer.PeerUser(config.telegram.lensID.value).userID
                  )
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
            req <- makeSendRequest(message)
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

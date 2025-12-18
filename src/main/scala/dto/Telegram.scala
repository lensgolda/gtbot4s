package dto

import zio.json.JsonCodec
import zio.schema.DeriveSchema
import zio.schema.Schema
import zio.schema.annotation.fieldName

final case class SendMessageRequest(
    text: String,
    @fieldName("chat_id") chatID: Long
) derives JsonCodec

object SendMessageRequest:
    given Schema[SendMessageRequest] = DeriveSchema.gen[SendMessageRequest]

package extensions

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import zio.*
import zio.http.Body

import scala.xml.*

object XmlCodec:
    extension (data: Body)
        def asXmlZIO: Task[Elem] = for
            bytes <- data.asArray
            bis <- ZIO.attempt(
              BufferedInputStream(ByteArrayInputStream(bytes))
            )
            xmlDoc <- ZIO.attempt(XML.load(bis))
        yield xmlDoc

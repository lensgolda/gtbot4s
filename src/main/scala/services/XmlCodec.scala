package services

import zio.ZIO
import zio.ZLayer
// import scala.xml._

trait XmlCodec {
  def decode[A](xml: String): ZIO[Any, Throwable, A]
}

final class XmlCodecLive extends XmlCodec:
    override def decode[A](xml: String): ZIO[Any, Throwable, A] = ???
        // ZIO.attempt(
            
        // )

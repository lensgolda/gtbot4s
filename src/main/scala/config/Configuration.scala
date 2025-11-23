package config

import java.io.IOException

import zio.*
import zio.config.*
import zio.config.derivation.name
import zio.config.magnolia.*
import zio.config.typesafe.*
import zio.schema.Schema

object Configuration:

    opaque type TelegramToken = String
    opaque type TelegramChatID = Long
    opaque type TelegramURL = String
    opaque type TelegramChatUpdatesURL = String
    opaque type TelegramUserID = Long

    opaque type WeatherApiKey = String
    opaque type WeatherApiURL = String

    opaque type CbrBaseURL = String
    opaque type CbrCurrency = String

    final case class TelegramConfig(
        token: TelegramToken,
        chatID: TelegramChatID,
        baseURL: TelegramURL,
        lensID: TelegramUserID,
        egolkaID: TelegramUserID,
        chatUpdatesUrl: String
    ):
        override def toString(): String =
            s"""
            |TelegramConfig(
            | token=***, 
            | chatID=***, 
            | baseURL=$baseURL, 
            | lensID=***,
            | egolkaID=***,
            | chatUpdatesUrl=$chatUpdatesUrl
            |)"""

    final case class WeatherApiConfig(
        apiKey: WeatherApiKey,
        baseURL: WeatherApiURL
    ):
        override def toString(): String =
            s"""
            |WeatherApiConfig(
            | apiKey=***, 
            | baseURL=$baseURL
            |)"""

    final case class CbrConfig(
        baseURL: CbrBaseURL,
        currencies: Set[CbrCurrency]
    ):
        override def toString(): String =
            s"""
            |CbrConfig(
            | currencies=$currencies, 
            | baseURL=$baseURL
            |)"""

    final case class GoogleCalendarConfig(
        keyFile: String,
        targetCalendar: String,
        daysRange: Int
    ):
        override def toString(): String =
            s"""
                    |GoogleCalendarConfig(
                    | keyFile=***,
                    | targetCalendar=$targetCalendar, 
                    | daysRange=$daysRange
                    |)"""

    case class AppConfig(
        telegram: TelegramConfig,
        weatherapi: WeatherApiConfig,
        cbr: CbrConfig,
        googleCalendar: GoogleCalendarConfig
    )

    object AppConfig:
        val appConfigZIO: Task[AppConfig] = for
            configSource <- ConfigProvider.fromResourcePathZIO(true)
            config <- configSource.load(
              deriveConfig[AppConfig].mapKey(toKebabCase)
            )
        yield config

        val layer: ZLayer[Any, Throwable, AppConfig] =
            ZLayer
                .fromZIO(appConfigZIO)
                .tapError(err => Console.printError(err))

    object TelegramURL:
        def apply(value: String): TelegramURL = value
        given Config[TelegramURL] =
            Config.string.map(TelegramURL(_))

    object TelegramChatUpdatesURL:
        def apply(value: String): TelegramChatUpdatesURL = value
        given Config[TelegramChatUpdatesURL] =
            Config.string.map(TelegramChatUpdatesURL(_))

    object TelegramChatID:
        def apply(value: Long): TelegramChatID = value
        given Config[TelegramChatID] =
            Config.long.map(TelegramChatID(_))
        extension (id: TelegramChatID) def value: Long = id

    object TelegramToken:
        def apply(value: String): TelegramToken = value
        given Config[TelegramToken] =
            Config.string.map(TelegramToken(_))

    object TelegramUserID:
        def apply(value: Long): TelegramUserID = value
        given Config[TelegramUserID] =
            Config.long.map(TelegramUserID(_))
        extension (id: TelegramUserID) def value: Long = id

    object WeatherApiKey:
        def apply(value: String): WeatherApiKey = value
        given Config[WeatherApiKey] =
            Config.string.map(WeatherApiKey(_))

    object WeatherApiURL:
        def apply(value: String): WeatherApiURL = value
        given Config[WeatherApiURL] =
            Config.string.map(WeatherApiURL(_))

    object CbrCurrency:
        def apply(value: String): CbrCurrency = value
        given Config[CbrCurrency] =
            Config.string.map(CbrCurrency(_))

    object CbrBaseURL:
        def apply(value: String): CbrBaseURL = value
        given Config[CbrBaseURL] =
            Config.string.map(CbrBaseURL(_))

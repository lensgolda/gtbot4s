package config

import java.io.IOException

import zio.*
import zio.config.*
import zio.config.derivation.name
import zio.config.magnolia.*
import zio.config.typesafe.*

object Configuration:

    opaque type TelegramToken = String
    opaque type TelegramChatID = String
    opaque type TelegramURL = String

    opaque type WeatherApiKey = String
    opaque type WeatherApiURL = String
    
    opaque type CbrBaseURL = String
    opaque type CbrCurrency = String

    final case class TelegramConfig(
        token: TelegramToken,
        chatID: TelegramChatID,
        baseURL: TelegramURL,
    )

    final case class WeatherApiConfig(
        apiKey: WeatherApiKey,
        baseURL: WeatherApiURL,
    )

    final case class CbrConfig(
        baseURL: CbrBaseURL,
        currencies: Set[CbrCurrency]
    )

    case class AppConfig(
        telegram: TelegramConfig,
        weatherapi: WeatherApiConfig,
        cbr: CbrConfig,
    )       

    object AppConfig:
        val appConfigZIO: Task[AppConfig] = for 
            configSource <- ConfigProvider.fromResourcePathZIO(true)
            config <- configSource.load(deriveConfig[AppConfig].mapKey(toKebabCase))
        yield config
        
        val layer: ZLayer[Any, Throwable, AppConfig] = 
            ZLayer.fromZIO(appConfigZIO)
                .tap(configEnv => 
                    val appConfig = configEnv.get[AppConfig]
                    ZIO.logInfo(s"Application started with config: $appConfig")
                )
                .tapError(err => Console.printError(err))

    object TelegramURL:
        def apply(value: String): TelegramURL = value
        given Config[TelegramURL] =
            Config.string.map(TelegramURL(_))

    object TelegramChatID:
        def apply(value: String): TelegramChatID = value
        given Config[TelegramChatID] =
            Config.string.map(TelegramChatID(_))

    object TelegramToken:
        def apply(value: String): TelegramToken = value
        given Config[TelegramToken] =
            Config.string.map(TelegramToken(_))

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

package com.news4you

import com.news4you.config.{AppConfigProvider, ConfigProvider}
import com.news4you.http.HttpClient
import com.news4you.http.HttpClient.{ClientTask, HttpClient}
import org.http4s.client.blaze.BlazeClientBuilder
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger
import zio.{Task, ZIO, ZLayer}

import scala.concurrent.ExecutionContext.Implicits

object Layers {
  type ConfigurationEnv = ConfigProvider with Logging with Clock with Blocking
  type AppConfigurationEnv = ConfigurationEnv with AppConfigProvider with ClientTask
  type News4YouEnv = AppConfigurationEnv with HttpClient

  object live {
    private def makeHttpClient =
      ZIO.runtime[Any].map { implicit rts =>
        BlazeClientBuilder
            .apply[Task](Implicits.global)
            .resource
            .toManaged
      }

    val configurationEnv: ZLayer[Blocking, Throwable, ConfigurationEnv] =
      Blocking.any ++ Clock.live ++ ConfigProvider.live ++ Slf4jLogger.make((_, msg) => msg) /*++ Slf4jLogger.make((_, msg) => msg)*/

    val appConfigurationEnv: ZLayer[ConfigurationEnv, Throwable, AppConfigurationEnv] =
      AppConfigProvider.fromConfig ++ ZLayer.fromManaged(makeHttpClient.toManaged_.flatten) ++ ZLayer.identity

    val appLayer: ZLayer[Blocking, Throwable, News4YouEnv] =
      configurationEnv >>> appConfigurationEnv >>> HttpClient.http4s ++ ZLayer.identity
  }

}

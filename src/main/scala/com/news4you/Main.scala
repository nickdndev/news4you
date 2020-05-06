package com.news4you

import canoe.api._
import cats.effect.ExitCode
import com.news4you.bot.News4YouBot
import com.news4you.config._
import fs2.Stream
import zio._
import zio.clock.Clock
import zio.interop.catz._

object Main extends App {
    type AppTask[A] = RIO[Layers.News4YouEnv with Clock, A]

    override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
        val prog =
            for {
                cfg <- ZIO.access[ConfigProvider](_.get)
                _ <- logging.log.info(s"Starting with $cfg")
                botConfig = cfg.botConfig
                _ <- runTelegramBot(botConfig.token)
            } yield 0

        prog.provideLayer(Layers.live.appLayer).orDie
    }

    def runTelegramBot[R <: Clock](token: String) = {
        type Task[A] = RIO[R, A]

        ZIO.runtime[R].flatMap { implicit rts =>
            Stream
                .resource(TelegramClient.global[Task](token))
                .flatMap { implicit client =>
                    Bot.polling[Task].follow(News4YouBot.news4YouBotScenario)
                }
                .compile.drain.as(ExitCode.Success)
        }
    }
}

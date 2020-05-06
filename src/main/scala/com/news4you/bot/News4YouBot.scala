package com.news4you.bot
import canoe.api.{Scenario, TelegramClient, _}
import canoe.syntax.{command, text, _}

object News4YouBot {
    def news4YouBotScenario[F[_] : TelegramClient]: Scenario[F, Unit] =
        for {
            chat <- Scenario.expect(command("hi").chat)
            _ <- Scenario.eval(chat.send("Hello. What's your name?"))
            name <- Scenario.expect(text)
            _ <- Scenario.eval(chat.send(s"Nice to meet you, $name"))
        } yield ()
}

package io.github.dimonchik0036.tcbot

import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.BaseResponse
import org.slf4j.LoggerFactory
import java.util.regex.PatternSyntaxException

interface BotCommand {
    val name: String
    val description: String
    val usage: String
    operator fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse

    fun TeamCityTelegramBot.sendTextMessage(text: String, chat: TelegramChat): BaseResponse =
        sender.execute(SendMessage(chat.id, text))

    fun TeamCityTelegramBot.sendTextMessageWithReply(text: String, chat: TelegramChat, message: Message): BaseResponse =
        sender.execute(SendMessage(chat.id, text).replyToMessageId(message.messageId()))
}

val BotCommand.help: String
    get() = "Command: $name\n" +
            "Usage: $usage\n" +
            "Description: $description"

val ALL_COMMANDS = listOf(
    HelpCommand(),
    LoginCommand(),
    LogoutCommand(),
    FilterCommand(),
    CheckFilterCommand(),
    CountCommand(),
    CommandsCommand()
).let {
    it.forEach { command ->
        if (command.name.length > 32) error("The command name must be no more than 32 characters")
    }
    it
}

private val LOG = LoggerFactory.getLogger("bot-command")

class LoginCommand : BotCommand {
    override val name: String = "login"
    override val usage: String = "/login [auth_key]"
    override val description: String = "Enable notifications"
    override fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse {
        val key = message.commandArguments
        val text = if (bot.checkAuthKey(key)) {
            chat.isAuth = true
            LOG.info("Notifications enabled in $chat")
            "Notifications enabled"
        } else {
            LOG.info("Attempting to enable notification with the wrong key `$key` in `$chat`")
            "Bad auth key"
        }
        return bot.sendTextMessageWithReply(text, chat, message)
    }
}

class LogoutCommand : BotCommand {
    override val name: String = "logout"
    override val usage: String = "/logout"
    override val description: String = "Disable notifications"
    override fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse {
        chat.isAuth = false
        LOG.info("Disable notification in $chat")
        return bot.sendTextMessageWithReply("Notifications disabled", chat, message)
    }
}

class HelpCommand : BotCommand {
    override val name: String = "help"
    override val usage: String = "/help [command_name]"
    override val description: String = "Show a command description"
    override fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse {
        val args = message.commandArguments
        return with(bot) {
            val text = if (args.isEmpty()) commands.values.joinToString(
                separator = "\n----------\n",
                transform = BotCommand::help
            ) else commands[args]?.help ?: "Couldn't find `$args` command"
            sendTextMessageWithReply(text, chat, message)
        }
    }
}

class FilterCommand : BotCommand {
    override val name: String = "filter"
    override val usage: String = "/filter <filter_name> <pattern>. Pattern example: `rr/.*`"
    override val description: String = "Add filtering"
    override fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse {
        val args = message.commandArguments
        val (_, filterGetter) = chat.filterProperty(args.substringBefore(' '))
        val text = if (filterGetter == null) help
        else createFilter(args.substringAfter(' ')) { filterGetter.set(it) }
        return bot.sendTextMessageWithReply(text, chat, message)
    }
}

private fun createFilter(pattern: String, onSuccess: (Regex) -> Unit): String = if (pattern.isEmpty()) "Empty pattern"
else try {
    val regex = Regex(pattern)
    onSuccess(regex)
    "Success"
} catch (e: PatternSyntaxException) {
    "Syntax error"
}

class CheckFilterCommand : BotCommand {
    override val name: String = "filter_check"
    override val usage: String = "/filter_check <filter_name> <text>"
    override val description: String = "Check filter"
    override fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse {
        val args = message.commandArguments
        val (ok, filterGetter) = chat.filterProperty(args.substringBefore(' '))
        val text = if (!ok) help else checkFilter(args.substringAfter(' '), filterGetter?.get())
        return bot.sendTextMessageWithReply(text, chat, message)
    }
}

private fun checkFilter(text: String, regex: Regex?): String = when {
    text.isEmpty() -> "Missing text"
    regex == null -> "No filter specified"
    else -> "Result: ${regex.matches(text)}"
}

class CountCommand : BotCommand {
    override val name: String = "count"
    override val usage: String = "/count <name>"
    override val description: String = "Get number of"
    override fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse {
        val text = when (message.commandArguments) {
            "running_builds" -> bot.service.runningBuildCount.toString()
            else -> help
        }
        return bot.sender.execute(SendMessage(chat.id, text).replyToMessageId(message.messageId()))
    }
}

class CommandsCommand : BotCommand {
    override val name: String = "commands"
    override val usage: String = "/commands"
    override val description: String = "Description like @BotFather"
    override fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse {
        val text = ALL_COMMANDS.joinToString(separator = "\n") { "${it.name} - ${it.description}" }
        return bot.sender.execute(SendMessage(chat.id, text).replyToMessageId(message.messageId()))
    }
}

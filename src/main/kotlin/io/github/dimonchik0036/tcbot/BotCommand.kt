package io.github.dimonchik0036.tcbot

import com.pengrad.telegrambot.model.Message
import org.slf4j.LoggerFactory
import java.util.regex.PatternSyntaxException

data class BotCommand(
    val name: String,
    val description: String,
    val usage: String,
    private val handler: BotCommand.(TeamCityTelegramBot, TelegramUser, TelegramChat, Message) -> Unit
) {
    operator fun invoke(bot: TeamCityTelegramBot, user: TelegramUser, chat: TelegramChat, message: Message) =
        handler(bot, user, chat, message)

    val help = "Command: $name\n" +
            "Usage: $usage\n" +
            "Description: $description"
}

val ALL_COMMANDS = listOf(
    BotCommand(
        name = "login",
        usage = "/login [auth_key]",
        description = "Enable notifications",
        handler = BotCommand::login
    ),
    BotCommand(
        name = "logout",
        usage = "/logout",
        description = "Disable notifications",
        handler = BotCommand::logout
    ),
    BotCommand(
        name = "help",
        usage = "/help [command_name]",
        description = "Show a command description",
        handler = BotCommand::help
    ),
    BotCommand(
        name = "filter",
        usage = "/filter <filter_name> <pattern>. Pattern example: `rr/.*`" + availableFilterName(),
        description = "Add filtering",
        handler = BotCommand::filter
    ),
    BotCommand(
        name = "filter_check",
        usage = "/filter_check <filter_name> <text>" + availableFilterName(),
        description = "Check filter",
        handler = BotCommand::filterCheck
    ),
    BotCommand(
        name = "count",
        usage = "/count <name>" + availableKeys("name", listOf("running_builds")),
        description = "Get number of",
        handler = BotCommand::count
    ),
    BotCommand(
        name = "commands",
        usage = "/commands",
        description = "Description like @BotFather",
        handler = BotCommand::commands
    )
).let {
    it.forEach { command ->
        if (command.name.length > 32) error("The command ${command.name} must be no more than 32 characters")
    }
    it
}

private val LOG = LoggerFactory.getLogger("bot-command")

private fun availableKeys(name: String, keys: List<String>, prefix: String = "\n"): String = keys
    .joinToString(
        prefix = "${prefix}Available keys for `$name`: [ ",
        postfix = " ]",
        separator = ", "
    ) { "`$it`" }

private fun availableFilterName(): String = availableKeys("filter_name", Filter.FILTER_NAMES)

private fun BotCommand.login(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    val key = message.commandArguments
    val text = if (bot.checkAuthKey(key)) {
        chat.isAuth = true
        LOG.info("Notifications enabled in $chat")
        "Notifications enabled"
    } else {
        LOG.info("Attempting to enable notification with the wrong key `$key` in `$chat`")
        "Bad auth key"
    }
    bot.sendTextMessageWithReply(text, chat, message)
}

private fun BotCommand.logout(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    chat.isAuth = false
    LOG.info("Disable notification in $chat")
    bot.sendTextMessageWithReply("Notifications disabled", chat, message)
}

private fun BotCommand.help(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    val args = message.commandArguments
    with(bot) {
        val text = if (args.isEmpty()) commands.values.joinToString(
            separator = "\n----------\n",
            transform = BotCommand::help
        ) else commands[args]?.help ?: "Couldn't find `$args` command"
        sendTextMessageWithReply(text, chat, message)
    }
}

private fun BotCommand.filter(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    val args = message.commandArguments
    val filter = chat.filter
    val filterName = args.substringBefore(' ')
    val text = if (!Filter.isFilterName(filterName)) help
    else createFilter(args.substringAfter(' ')) { filter.setFilterByName(filterName, it) }
    bot.sendTextMessageWithReply(text, chat, message)
}

private fun createFilter(pattern: String, onSuccess: (Regex) -> Unit): String = if (pattern.isEmpty()) "Empty pattern"
else try {
    val regex = Regex(pattern)
    onSuccess(regex)
    "Success"
} catch (e: PatternSyntaxException) {
    "Syntax error"
}

private fun BotCommand.filterCheck(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    val args = message.commandArguments
    val filter = chat.filter
    val (ok, regex) = filter.getFilterByName(args.substringBefore(' '))
    val text = if (!ok) help else checkFilter(args.substringAfter(' '), regex)
    bot.sendTextMessageWithReply(text, chat, message)
}

private fun checkFilter(text: String, regex: Regex?): String = when {
    text.isEmpty() -> "Missing text"
    regex == null -> "No filter specified"
    else -> "Result: ${regex.matches(text)}"
}

private fun BotCommand.count(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    val text = when (message.commandArguments) {
        "running_builds" -> bot.service.runningBuildCount.toString()
        else -> help
    }
    bot.sendTextMessageWithReply(text, chat, message)
}

private fun BotCommand.commands(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    val text = ALL_COMMANDS.joinToString(separator = "\n") { "${it.name} - ${it.description}" }
    bot.sendTextMessageWithReply(text, chat, message)
}

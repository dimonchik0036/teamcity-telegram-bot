package io.github.dimonchik0036.tcbot

import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import org.slf4j.LoggerFactory
import java.util.regex.PatternSyntaxException

data class BotCommand(
    val name: String,
    val description: String,
    val usage: String,
    val auth: Boolean = false,
    private val handler: BotCommand.(TeamCityTelegramBot, TelegramUser, TelegramChat, Message) -> Unit
) {
    operator fun invoke(bot: TeamCityTelegramBot, user: TelegramUser, chat: TelegramChat, message: Message) =
        if (checkPermissions(user, chat)) handler(bot, user, chat, message)
        else {
            LOG.info("Not permitted launch $name to $user in $chat")
            bot.sendTextMessage(NOT_PERMITTED, chat, message)
        }

    private fun checkPermissions(user: TelegramUser, chat: TelegramChat): Boolean = !auth || chat.isAuth

    val help = "Command: $name\n" +
            "Usage: $usage\n" +
            "Description: $description"

    companion object {
        private const val SUCCESS_EMOJI = "\u2705"
        private const val FAILED_EMOJI = "\u274c"
        private const val NOT_PERMITTED_EMOJI = "\ud83d\udeab"
        const val SUCCESS_AUTH = "$SUCCESS_EMOJI Notifications enabled"
        const val FAILED_AUTH = "$FAILED_EMOJI Bad auth key"
        const val NOT_PERMITTED = "$NOT_PERMITTED_EMOJI Not permitted"
        const val LOGOUT_MESSAGE = "Notifications disabled"
        const val SUCCESS = "$SUCCESS_EMOJI Success"
        const val FAILED = "$FAILED_EMOJI Failed"
    }
}

val ALL_COMMANDS = listOf(
    BotCommand(
        name = "login",
        usage = "/login [auth_key]",
        description = "Sign in",
        handler = BotCommand::login
    ),
    BotCommand(
        name = "logout",
        usage = "/logout",
        description = "Sign out",
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
        handler = BotCommand::filter,
        auth = true
    ),
    BotCommand(
        name = "filter_check",
        usage = "/filter_check <filter_name> <text>" + availableFilterName(),
        description = "Check filter",
        handler = BotCommand::filterCheck,
        auth = true
    ),
    BotCommand(
        name = "count",
        usage = "/count <name>" + availableKeys("name", listOf("running_builds")),
        description = "Get number of",
        handler = BotCommand::count,
        auth = true
    ),
    BotCommand(
        name = "commands",
        usage = "/commands",
        description = "Description like @BotFather",
        handler = BotCommand::commands
    ),
    BotCommand(
        name = "running",
        usage = "/running [filter]" + availableKeys("filter", listOf("all")),
        description = "Show running builds",
        handler = BotCommand::running,
        auth = true
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
        BotCommand.SUCCESS_AUTH
    } else {
        LOG.info("Attempting to enable notification with the wrong key `$key` in `$chat`")
        BotCommand.FAILED_AUTH
    }
    bot.sendTextMessage(text, chat, message)
}

private fun BotCommand.logout(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    chat.isAuth = false
    LOG.info("Disable notification in $chat")
    bot.sendTextMessage(BotCommand.LOGOUT_MESSAGE, chat, message)
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
        sendTextMessage(text, chat, message)
    }
}

private fun BotCommand.filter(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    val args = message.commandArgumentsList
    val filter = chat.filter
    val filterName = args.firstOrNull() ?: ""
    val pattern = args.getOrNull(1)
    val text = if (!Filter.isFilterName(filterName) || pattern == null) help
    else createFilter(pattern) { filter.setFilterByName(filterName, it) }
    bot.sendTextMessage(text, chat, message)
}

private fun createFilter(pattern: String, onSuccess: (Regex) -> Unit): String = try {
    val regex = Regex(pattern)
    onSuccess(regex)
    BotCommand.SUCCESS
} catch (e: PatternSyntaxException) {
    "Syntax error"
}

private fun BotCommand.filterCheck(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    val args = message.commandArgumentsList
    val filter = chat.filter
    val regex = filter.getFilterByName(args.firstOrNull() ?: "")
    val textForCheck = args.getOrNull(1)
    val text = if (regex == null || textForCheck == null) help
    else "${regex.matches(textForCheck)}"
    bot.sendTextMessage(text, chat, message)
}

private fun BotCommand.count(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    val text = when (message.commandArguments) {
        "running_builds" -> bot.service.runningBuilds.size.toString()
        else -> help
    }
    bot.sendTextMessage(text, chat, message)
}

private fun BotCommand.commands(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    val text = ALL_COMMANDS.joinToString(separator = "\n") { "${it.name} - ${it.description}" }
    bot.sendTextMessage(text, chat, message)
}

private fun BotCommand.running(
    bot: TeamCityTelegramBot,
    user: TelegramUser,
    chat: TelegramChat,
    message: Message
) {
    val filter = chat.filter
    val builds = when (message.commandArguments) {
        "all" -> bot.service.runningBuilds.asSequence()
        "" -> bot.service.runningBuilds.asSequence().filter(filter::matches)
        else -> null
    }
    with(bot) {
        when {
            builds == null -> sendTextMessage(help, chat, message)
            builds.none() -> sendTextMessage("Builds not fount", chat, message)
            // Telegram can't mark up a completely large message.
            else -> builds.chunked(10).forEach {
                val text = it.joinToString(separator = "\n\n", transform = TeamCityBuild::description)
                sendTextMessage(text, chat, message)
            }
        }
    }
}

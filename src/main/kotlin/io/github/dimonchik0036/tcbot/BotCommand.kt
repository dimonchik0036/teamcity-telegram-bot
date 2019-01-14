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
}

val BotCommand.help: String
    get() = "Command: $name\n" +
            "Usage: $usage\n" +
            "Description: $description"

val ALL_COMMANDS = listOf(
    HelpCommand(),
    LoginCommand(),
    LogoutCommand(),
    BranchFilterCommand(),
    CheckBranchFilterCommand(),
    BuildFilterCommand(),
    CheckBuildFilterCommand()
).let {
    it.forEach { command ->
        if (command.name.length > 32) error("The command name must be no more than 32 characters")
    }
    it
}

private val LOG = LoggerFactory.getLogger("bot-command")

data class CommandContext(private val context: HashMap<String, Any> = hashMapOf()) {
    fun clear() = context.clear()
    operator fun get(key: String): Any? = context[key]
    operator fun set(key: String, value: Any) {
        context[key] = value
    }
}

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
        return bot.sender.execute(SendMessage(chat.id, text))
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
        return bot.sender.execute(SendMessage(chat.id, "Notifications disabled"))
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
            sender.execute(SendMessage(chat.id, text))
        }
    }
}

class BranchFilterCommand : BotCommand {
    override val name: String = "branch_filter"
    override val usage: String = "/branch_filter <pattern>. Pattern example: `rr/.*`"
    override val description: String = "Add filtering by branch"
    override fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse {
        val text = createFilter(message.commandArguments) { chat.branchFilter = it }
        return bot.sender.execute(SendMessage(chat.id, text))
    }
}

class CheckBranchFilterCommand : BotCommand {
    override val name: String = "branch_filter_check"
    override val usage: String = "/branch_filter_check <branch_name>"
    override val description: String = "Check current branch filter"
    override fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse {
        val text = checkFilter(message.commandArguments, chat.branchFilter)
        return bot.sender.execute(SendMessage(chat.id, text))
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

private fun checkFilter(text: String, regex: Regex?): String = when {
    text.isEmpty() -> "Missing branch name"
    regex == null -> "No filter specified"
    else -> "Result: ${regex.matches(text)}"
}

class BuildFilterCommand : BotCommand {
    override val name: String = "build_filter"
    override val usage: String = "/build_filter <pattern>. Pattern example: `Kotlin_dev_Aggregate.*`"
    override val description: String = "Add filtering by build configuration id"
    override fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse {
        val text = createFilter(message.commandArguments) { chat.buildFilter = it }
        return bot.sender.execute(SendMessage(chat.id, text))
    }

}

class CheckBuildFilterCommand : BotCommand {
    override val name: String = "build_filter_check"
    override val usage: String = "/build_filter_check <build_config_id>"
    override val description: String = "Check current build filter"
    override fun invoke(
        bot: TeamCityTelegramBot,
        user: TelegramUser,
        chat: TelegramChat,
        message: Message
    ): BaseResponse {
        val text = checkFilter(message.commandArguments, chat.buildFilter)
        return bot.sender.execute(SendMessage(chat.id, text))
    }
}

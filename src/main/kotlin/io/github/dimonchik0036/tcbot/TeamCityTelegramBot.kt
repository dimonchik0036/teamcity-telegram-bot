package io.github.dimonchik0036.tcbot

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.User
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.BaseResponse
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory


private val LOG = LoggerFactory.getLogger("telegram-bot")

class TeamCityTelegramBot(
    token: String,
    val commands: Map<String, BotCommand>,
    private val creatorId: Long,
    private val authKey: String? = null
) {
    lateinit var service: TeamCityService
    val sender = TelegramBot(token)
    val userManager = Manager<Int, TelegramUser>()
    val chatManager = Manager<Long, TelegramChat>()

    fun start(service: TeamCityService) {
        this.service = service
        LOG.debug("Start bot")
        try {
            sender.execute(SendMessage(creatorId, "Run"))
        } catch (e: Exception) {
            throw RuntimeException("Couldn't send message to creator", e)
        }

        sender.setUpdatesListener { updates ->
            LOG.debug("Get updates")
            updates.forEach { update ->
                GlobalScope.launch {
                    onUpdate(update)
                }
            }
            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }

    fun buildHandlers(): Map<String, BuildHandler> = mapOf(
        "build info" to { build ->
            val description = createBuildDescriptionMarkdown(this, build)
            val branchName = build.branch.name
            GlobalScope.launch {
                chatManager.forEach { _, chat ->
                    if (!chat.isAuth) return@forEach
                    if (chat.buildFilter?.matches(build.buildConfigurationId.stringId) == false) return@forEach
                    if (branchName == null || chat.branchFilter?.matches(branchName) == false) return@forEach
                    sender.execute(SendMessage(chat.id, description).parseMode(ParseMode.Markdown))
                }
            }
        }
    )

    fun checkAuthKey(key: String?): Boolean = if (authKey.isNullOrBlank()) true
    else authKey == key

    private fun onUpdate(update: Update) {
        LOG.info("New update $update")
        try {
            val message: Message? = update.message()
            if (message != null) {
                val chat = getChat(message.chat())
                val user = getUser(message.from())
                onMessage(user, chat, message)
            }
        } catch (e: Exception) {
            LOG.warn("Couldn't handle with update $update", e)
        }
    }

    private fun getChat(chat: Chat): TelegramChat = chatManager.getOrPut(chat.id()) {
        LOG.info("New chat $chat")
        TelegramChat.fromChat(chat)
    }

    private fun getUser(user: User): TelegramUser = userManager.getOrPut(user.id()) {
        LOG.info("New user $user")
        TelegramUser.fromUser(user)
    }

    private fun onMessage(user: TelegramUser, chat: TelegramChat, message: Message) {
        val command = message.command
        if (command.isEmpty()) return
        val handler = commands[command]
        if (handler != null) {
            LOG.info("Run $command")
            handler(this, user, chat, message).checkError(chat)
            user.lastCommand = handler
        } else {
            LOG.info("Unknown command $command")
        }
    }
}

fun BaseResponse.checkError(chat: TelegramChat) {
    if (!isOk) LOG.warn("Error response in $chat chat: $this")
}
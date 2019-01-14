package io.github.dimonchik0036.tcbot

import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.User
import java.util.concurrent.ConcurrentHashMap

class Manager<Key, Value> {
    private val map: ConcurrentHashMap<Key, Value> = ConcurrentHashMap()
    fun getOrPut(key: Key, defaultValue: () -> Value): Value = map.getOrPut(key, defaultValue)
    operator fun get(key: Key): Value = map[key] ?: error("Couldn't find $key")

    fun forEach(action: (Key, Value) -> Unit) = map.forEach(action)
}

class TelegramUser(
    val id: Int,
    val username: String?,
    val firstName: String,
    val lastName: String?
) {
    @Volatile
    var lastCommand: BotCommand? = null
    val context: CommandContext = CommandContext()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return id == (other as TelegramUser).id
    }

    override fun hashCode(): Int = id
    override fun toString(): String = "User(id=$id, name=${createName(username, firstName, lastName)})"

    companion object {
        fun fromUser(user: User): TelegramUser = TelegramUser(
            id = user.id(),
            username = user.username(),
            firstName = user.firstName(),
            lastName = user.lastName()
        )
    }
}

class TelegramChat(
    val id: Long,
    val title: String?,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    @Volatile
    var isAuth: Boolean = false,
    @Volatile
    var branchFilter: Regex? = null,
    @Volatile
    var buildFilter: Regex? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return id == (other as TelegramChat).id
    }

    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Chat(id=$id, title=${createName(username, firstName, lastName, title)})"

    companion object {
        fun fromChat(chat: Chat): TelegramChat = TelegramChat(
            id = chat.id(),
            username = chat.username(),
            firstName = chat.firstName(),
            lastName = chat.lastName(),
            title = chat.title()
        )
    }
}

private fun createName(
    username: String? = null,
    firstName: String? = null,
    lastName: String? = null,
    title: String? = null,
    defaultValue: () -> String = { "unknown" }
): String = when {
    username != null -> "@$username"
    title != null -> title
    firstName != null -> if (lastName != null) "$firstName $lastName" else firstName
    else -> defaultValue()
}

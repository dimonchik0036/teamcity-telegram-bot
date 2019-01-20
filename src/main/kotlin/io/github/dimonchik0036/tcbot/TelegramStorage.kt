package io.github.dimonchik0036.tcbot

import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.User
import java.util.concurrent.ConcurrentHashMap

class TelegramStorage<Key, Value> {
    private val map: ConcurrentHashMap<Key, Value> = ConcurrentHashMap()
    fun getOrPut(key: Key, defaultValue: () -> Value): Value = map.getOrPut(key, defaultValue)
    operator fun get(key: Key): Value? = map[key]
    fun getValue(key: Key): Value = map[key] ?: error("Couldn't find $key")
    operator fun set(key: Key, value: Value) {
        map[key] = value
    }

    fun remove(key: Key): Value? = map.remove(key)
    val entries: Sequence<Map.Entry<Key, Value>> get() = map.asSequence()

    fun forEach(action: (Key, Value) -> Unit) = map.forEach(action)
}

class TelegramUser(
    val id: Int,
    val username: String?,
    val firstName: String,
    val lastName: String?
) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        else -> id == (other as TelegramUser).id
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
    val filter: Filter = Filter(),
    @Volatile
    var isAuth: Boolean = false
) {
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        else -> id == (other as TelegramChat).id
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

package io.github.dimonchik0036.tcbot

import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.MessageEntity

val Message.isCommand: Boolean
    get() {
        val entities = entities()
        if (entities.isNullOrEmpty()) return false
        val entity = entities.first()
        return 0 == entity.offset() && MessageEntity.Type.bot_command == entity.type()
    }

val Message.commandWithAt: String
    get() {
        if (!isCommand) return ""
        val entity = entities().first()
        return text().substring(1, entity.length())
    }

val Message.command: String get() = commandWithAt.substringBefore("@")

val Message.commandArguments: String
    get() {
        if (!isCommand) return ""
        val entity = entities().first()
        val text = text()
        return if (text.length == entity.length()) ""
        else text.substring(entity.length()).trim()
    }
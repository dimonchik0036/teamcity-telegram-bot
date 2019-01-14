package io.github.dimonchik0036.tcbot.core.telegram

import io.requery.Entity

interface TelegramType

@Entity
interface GGG : TelegramType{
    val name: String
    val id: Int
}
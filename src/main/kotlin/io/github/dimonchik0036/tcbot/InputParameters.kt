package io.github.dimonchik0036.tcbot

class InputParameters(args: Array<String>) {
    private val values = args.associate { it.substringBefore('=') to it.substringAfter('=') }
    val databasePath: String? = values["db"]
    val creatorId: Long? = values["creator"]?.toLongOrNull()
    val configPath: String? = values["config"]
}
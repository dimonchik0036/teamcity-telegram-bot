package io.github.dimonchik0036.tcbot

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.IOException
import java.time.Instant
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = runBlocking {
    try {
        val config = Config(inputValues(args))
        val handler = BuildHandler(this)
        val teamCityService = TeamCityService(
            serverUrl = config.serverUrl,
            checkUpdatesDelayMillis = config.updatesDelay ?: 30_000,
            checkProjectDelayMillis = config.projectsDelay ?: 600_000,
            rootProjectsId = config.rootProjectId ?: emptySet(),
            lastUpdate = Instant.now(),
            buildHandler = handler,
            authType = config.authType ?: Guest
        )

        val bot = TeamCityTelegramBot(
            token = config.token,
            creatorId = config.creatorId,
            authKey = config.authKey,
            commands = ALL_COMMANDS.associate { it.name to it },
            teamCityService = teamCityService
        )

        handler.handlers = bot.buildHandlers()
        bot.start()
    } catch (e: Exception) {
        println(e)
        LoggerFactory.getLogger("main").warn("An error occurred during operation", e)
        exitProcess(1)
    }
}

private fun inputValues(args: Array<String>): Map<String, String> {
    val properties = Properties()
    val path = args.firstOrNull() ?: error("Usage: java -jar tcbot-<version>.jar <path/to/config>")
    try {
        properties.load(FileInputStream(path))
    } catch (e: IOException) {
        error("Couldn't read properties: ${e.message}")
    }

    return properties.mapNotNull {
        val key = it.key as? String ?: return@mapNotNull null
        val value = it.value as? String ?: return@mapNotNull null
        key to value
    }.toMap()
}

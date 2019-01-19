package io.github.dimonchik0036.tcbot

import org.jetbrains.teamcity.rest.ProjectId

class Config(private val map: Map<String, String>) {
    val botToken: String = require("bot_token")
    val serverUrl: String = require("server_url")
    val teamCityUser: TeamCityUser = teamCityUser() ?: Guest
    val creatorId: Long = require("creator_id", String::toLong)
    val authKey: String? = option("auth_key")
    val updatesDelay: Long = option("updates_delay", String::toLong) ?: 30_000
    val projectsDelay: Long = option("projects_delay", String::toLong) ?: 600_000
    val cascadeMode: CascadeMode = option("cascade_mode") { CascadeMode.valueOf(this) } ?: CascadeMode.ONLY_ROOT
    val rootProjectId: Set<ProjectId> = option("root_project_id") {
        split(' ').map(String::trim).filterNot(String::isEmpty).map { ProjectId(it) }.toSet()
    } ?: emptySet()

    private fun teamCityUser(): TeamCityUser? {
        val username: String = option("teamcity_username") ?: return null
        val password: String = require("teamcity_password")
        return AuthorizedUser(username, password)
    }

    private fun <R> option(key: String, transform: String.() -> R): R? = option(key)?.transform()
    private fun option(key: String): String? = map[key]
    private fun <R> require(key: String, transform: String.() -> R): R = require(key).transform()
    private fun require(key: String): String = map[key] ?: error("Couldn't find `$key` in config")
}


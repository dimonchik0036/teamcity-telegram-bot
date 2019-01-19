package io.github.dimonchik0036.tcbot

import org.jetbrains.teamcity.rest.*

class TeamCityBuild(
    val id: String,
    val url: String,
    val name: String,
    val number: String?,
    val state: BuildState,
    val lastAuthor: String?,
    val branchName: String?,
    val status: BuildStatus?,
    val buildConfigurationId: String
) {
    val markdownDescription: String = "Build name: *$name*\n" +
            "Build number: *${number ?: "unknown"}*\n" +
            "Build state: *${state.name}*\n" +
            "Build status: *${status ?: "CANCELED"}*\n" +
            "Branch name: *${branchName ?: "unknown"}*\n" +
            "Last author: *${lastAuthor ?: "unknown"}*\n" +
            "Full description: [open]($url)"

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        else -> id != (other as TeamCityBuild).id
    }

    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "TeamCityBuild(id=$id, name=$name, url=$url)"

    companion object {
        fun fromBuild(build: Build) = TeamCityBuild(
            id = build.id.stringId,
            name = build.name,
            number = build.buildNumber,
            branchName = build.branch.name,
            lastAuthor = build.changes.firstOrNull()?.username,
            status = build.status,
            state = build.state,
            url = build.getHomeUrl(),
            buildConfigurationId = build.buildConfigurationId.stringId
        )
    }
}

typealias TeamCityBuildHandler = (TeamCityBuild) -> Unit

sealed class TeamCityUser {
    fun teamCityInstance(serverUrl: String): TeamCityInstance = when (this) {
        Guest -> TeamCityInstanceFactory.guestAuth(serverUrl)
        is AuthorizedUser -> TeamCityInstanceFactory.httpAuth(serverUrl, username, password)
    }
}

object Guest : TeamCityUser()
class AuthorizedUser(val username: String, val password: String) : TeamCityUser()

enum class CascadeMode {
    RECURSIVELY,
    ONLY_ROOT
}
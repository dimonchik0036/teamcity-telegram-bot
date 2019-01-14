package io.github.dimonchik0036.tcbot.core.teamcity

import org.jetbrains.teamcity.rest.ProjectId

interface TeamCityConfig {
    val rootProjectId: List<ProjectId>
    val authType: AuthType
    val checkDelayMillis: Long
}

sealed class AuthType
object Guest : AuthType()
class WithPassword(val username: String, val password: String) : AuthType()
package io.github.dimonchik0036.tcbot.core.teamcity.impl

import io.github.dimonchik0036.tcbot.core.teamcity.AuthType
import io.github.dimonchik0036.tcbot.core.teamcity.Guest
import io.github.dimonchik0036.tcbot.core.teamcity.TeamCityConfig
import org.jetbrains.teamcity.rest.ProjectId

class TeamCityConfigImpl(
    override val rootProjectId: List<ProjectId> = DefaultTeamCityConfig.rootProjectId,
    override val authType: AuthType = DefaultTeamCityConfig.authType,
    override val checkDelayMillis: Long = DefaultTeamCityConfig.checkDelayMillis
) : TeamCityConfig

object DefaultTeamCityConfig : TeamCityConfig {
    override val rootProjectId: List<ProjectId> = emptyList()
    override val authType: AuthType =
        Guest
    override val checkDelayMillis: Long = 60_000
}
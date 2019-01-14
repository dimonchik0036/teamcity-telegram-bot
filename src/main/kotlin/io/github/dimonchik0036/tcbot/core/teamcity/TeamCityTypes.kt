package io.github.dimonchik0036.tcbot.core.teamcity

import io.github.dimonchik0036.tcbot.core.teamcity.impl.BuildConfigurationIdConverter
import io.github.dimonchik0036.tcbot.core.teamcity.impl.ProjectIdConverter
import io.requery.Column
import io.requery.Convert
import io.requery.Entity
import io.requery.Key
import org.jetbrains.teamcity.rest.BuildConfigurationId
import org.jetbrains.teamcity.rest.ProjectId

interface TeamCityType

@Entity
interface TeamCityProject : TeamCityType {
    @get:Key
    @get:Convert(ProjectIdConverter::class)
    @get:Column(nullable = false, unique = true)
    var id: ProjectId

    @get:Column(nullable = false)
    var name: String

    @get:Column(nullable = false)
    var archived: Boolean

    @get:Convert(ProjectIdConverter::class)
    var parentId: ProjectId?

    val buildConfigurations: List<TeamCityBuildConfiguration>
}

@Entity
interface TeamCityBuildConfiguration : TeamCityType {
    @get:Key
    @get:Column(nullable = false, unique = true)
    @get:Convert(BuildConfigurationIdConverter::class)
    var id: BuildConfigurationId

    @get:Column(nullable = false)
    var name: String

    @get:Column(nullable = false)
    @get:Convert(ProjectIdConverter::class)
    var projectId: ProjectId

    @get:Column(nullable = false)
    var paused: Boolean
}
package io.github.dimonchik0036.tcbot

import org.jetbrains.teamcity.rest.Build
import org.jetbrains.teamcity.rest.BuildConfiguration
import org.jetbrains.teamcity.rest.Project

data class BuildContext(
    val project: Project,
    val configuration: BuildConfiguration
)

typealias BuildHandler = BuildContext.(Build) -> Unit

fun createBuildDescriptionMarkdown(context: BuildContext, build: Build): String {
    val buildName = build.name
    build.canceledInfo?.cancelDateTime
    val buildNumber = build.buildNumber ?: "unknown"
    val branchName = build.branch.name ?: "unknown"
    val lastAuthor = build.changes.firstOrNull()?.username ?: "unknown"
    val buildStatus = build.status?.name ?: "CANCELED"
    val buildState = build.state.name
    val url = build.getHomeUrl()
    return "Build name: *$buildName*\n" +
            "Build number: *$buildNumber*\n" +
            "Build state: *$buildState*\n" +
            "Build status: *$buildStatus*\n" +
            "Branch name: *$branchName*\n" +
            "Last author: *$lastAuthor*\n" +
            "Full description: [open]($url)"
}

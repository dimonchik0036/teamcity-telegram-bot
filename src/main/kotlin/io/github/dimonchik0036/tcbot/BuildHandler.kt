package io.github.dimonchik0036.tcbot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.teamcity.rest.Build
import org.jetbrains.teamcity.rest.BuildConfiguration
import org.jetbrains.teamcity.rest.Project
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("build-handler")

typealias RunningBuild = Pair<BuildContext, Build>

class BuildContextHandler(
    private val key: String,
    private val handler: BuildContext.(Build) -> Unit
) {
    operator fun invoke(context: BuildContext, build: Build) {
        LOG.debug("Invoke `$key` $build $context")
        handler(context, build)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (javaClass != other?.javaClass) false
        else key == (other as BuildContextHandler).key
    }

    override fun hashCode(): Int = key.hashCode()
    override fun toString(): String = "BuildContextHandler(key=$key)"
}

data class BuildContext(
    val project: Project,
    val configuration: BuildConfiguration
)

class BuildHandler(
    private val workerScope: CoroutineScope,
    @Volatile
    var handlers: Set<BuildContextHandler> = emptySet()
) {
    operator fun invoke(buildContext: BuildContext, build: Build) {
        handlers.forEach { handler ->
            workerScope.launch {
                handler(buildContext, build)
            }
        }
    }
}

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

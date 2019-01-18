package io.github.dimonchik0036.tcbot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.teamcity.rest.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

private val LOG = LoggerFactory.getLogger("team-city-service")

class TeamCityService(
    serverUrl: String,
    authType: AuthType = Guest,
    private var lastUpdate: Instant = Instant.now(),
    private val checkUpdatesDelayMillis: Long = 60_000,
    private val checkProjectDelayMillis: Long = 600_000,
    private val rootProjectsId: Set<ProjectId> = emptySet(),
    val buildHandler: BuildHandler
) {
    private val teamCityInstance = authType.teamCityInstance(serverUrl)
    private val runningBuilds = hashSetOf<RunningBuild>()

    val rooProjects: List<Project>
        get() = if (rootProjectsId.isEmpty()) listOf(teamCityInstance.rootProject())
        else rootProjectsId.map(teamCityInstance::project)

    init {
        LOG.debug("Check parameters")
        check(checkUpdatesDelayMillis >= 0) { "checkUpdatesDelayMillis is negative" }
        check(checkProjectDelayMillis >= 0) { "checkProjectDelayMillis is negative" }
        LOG.debug("Check all roots")
        rooProjects.forEach {
            LOG.debug("Check ${it.name}")
        }
    }

    var projectStructure: Map<Project, List<BuildConfiguration>> by RareUpdateLock(emptyMap())
        private set

    var vcsRoots: Set<VcsRoot> by RareUpdateLock(emptySet())
        private set

    fun start(scope: CoroutineScope): Job = scope.launch {
        LOG.debug("Start")
        launch { startCheckUpdates() }
        launch { startCheckProjectStructure() }
    }

    private suspend fun startCheckUpdates() {
        while (true) {
            try {
                checkUpdates()
            } catch (e: Exception) {
                LOG.warn("Error on check updates", e)
            }
            delay(checkUpdatesDelayMillis)
        }
    }

    private fun checkUpdates() {
        LOG.debug("Start check updates")
        checkRunningBuilds()
        checkNewBuilds()
        LOG.debug("End check updates")
    }

    private var lastBuildId: Long = 0
    private fun checkNewBuilds() {
        LOG.debug("Start check new builds (lastId=$lastBuildId, time=$lastUpdate)")
        var lastTime: ZonedDateTime = lastUpdate.atZone(ZoneId.of("UTC"))

        val oldMaxId: Long = lastBuildId
        projectStructure.forEach { (project, configurations) ->
            LOG.debug("Start check $project")
            configurations.forEach { buildConfiguration ->
                getNewBuilds(buildConfiguration.id, lastUpdate).forEach inner@{ build ->
                    val id = build.id.stringId.toLong()
                    if (id <= oldMaxId) return@inner
                    if (id > lastBuildId) lastBuildId = id

                    LOG.debug("New build $build")
                    if (build.state == BuildState.RUNNING) runningBuilds += BuildContext(
                        project,
                        buildConfiguration
                    ) to build

                    val startTime = build.startDateTime
                    if (startTime != null && startTime > lastTime) lastTime = startTime

                    buildHandler(BuildContext(project, buildConfiguration), build)
                }
            }
        }

        lastUpdate = lastTime.toInstant()
        LOG.debug("End check new builds")
    }

    private fun checkRunningBuilds() {
        LOG.debug("Start check running builds")
        runningBuilds.removeIf { (context, build) ->
            val newResult = teamCityInstance.build(build.id)
            if (newResult.state == BuildState.RUNNING) return@removeIf false
            LOG.debug("Build $newResult is finish")
            buildHandler(context, newResult)
            true
        }
        LOG.debug("End check running builds")
    }

    private fun getNewBuilds(configurationId: BuildConfigurationId, since: Instant): Sequence<Build> =
        teamCityInstance.builds()
            .fromConfiguration(configurationId)
            .includeCanceled()
            .includeFailed()
            .includeRunning()
            .withAllBranches()
            .since(since) // TODO: Replace with sinceBuild. Now this filter is not implemented in the teamcity-rest-client.
            .all()

    private suspend fun startCheckProjectStructure() {
        while (true) {
            try {
                checkProjectStructure()
            } catch (e: Exception) {
                LOG.warn("Error on check project structure", e)
            }
            delay(checkProjectDelayMillis)
        }
    }

    private tailrec fun fillProjectStructure(
        projects: List<Project>,
        map: HashMap<Project, List<BuildConfiguration>>
    ) {
        LOG.debug("Fill $projects")
        if (projects.isEmpty()) return
        projects.forEach { project ->
            map[project] = project.buildConfigurations
        }
        fillProjectStructure(projects.flatMap(Project::childProjects), map)
    }

    private fun checkProjectStructure() {
        LOG.debug("Start check project structure")

        val map = hashMapOf<Project, List<BuildConfiguration>>()
        fillProjectStructure(rooProjects, map)
        projectStructure = map

        vcsRoots = teamCityInstance.vcsRoots().all().toSet()

        LOG.debug("End check project structure")
    }
}

sealed class AuthType {
    fun teamCityInstance(serverUrl: String): TeamCityInstance = when (this) {
        Guest -> TeamCityInstanceFactory.guestAuth(serverUrl)
        is WithPassword -> TeamCityInstanceFactory.httpAuth(serverUrl, username, password)
    }
}

object Guest : AuthType()
class WithPassword(val username: String, val password: String) : AuthType()
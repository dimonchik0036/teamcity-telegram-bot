package io.github.dimonchik0036.tcbot

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.teamcity.rest.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val LOG = LoggerFactory.getLogger("team-city-service")

class TeamCityService(
    serverUrl: String,
    teamCityUser: TeamCityUser = Guest,
    private val cascadeMode: CascadeMode = CascadeMode.ONLY_ROOT,
    private val handlers: Map<String, BuildHandler>,
    private var lastUpdate: Instant = Instant.now(),
    private val checkUpdatesDelayMillis: Long = 60_000,
    private val checkProjectDelayMillis: Long = 600_000,
    private val rootProjectsId: Set<ProjectId> = emptySet()
) {
    private val teamCityInstance = teamCityUser.teamCityInstance(serverUrl)
    private val myRunningBuilds: ConcurrentHashMap<Build, BuildContext> = ConcurrentHashMap()

    private val rooProjects: List<Project>
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

    fun runningBuilds(filter: (Map.Entry<Build, BuildContext>) -> Boolean): Map<Build, BuildContext> =
        myRunningBuilds.filter(filter)

    @Volatile
    var runningBuildCount: Int = 0
        private set

    @Volatile
    var projectStructure: Map<Project, List<BuildConfiguration>> = emptyMap()
        private set

    @Volatile
    var vcsRoots: Set<VcsRoot> = emptySet()
        private set

    fun start() = runBlocking<Unit> {
        LOG.info("Start")
        launch { startCheckProjectStructure() }
        launch { startCheckUpdates() }
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
        checkNewBuilds(this::getNewBuilds)
        runningBuildCount = myRunningBuilds.count()
        LOG.debug("End check updates")
    }

    private var lastBuildId: Long = 0
    private fun checkNewBuilds(getter: (BuildConfigurationId) -> Sequence<Build>) {
        LOG.debug("Start check new builds (lastId=$lastBuildId, time=$lastUpdate)")
        var lastTime: ZonedDateTime = lastUpdate.atZone(ZoneId.of("UTC"))

        val oldMaxId: Long = lastBuildId
        projectStructure.forEach { (project, configurations) ->
            LOG.debug("Start check $project")
            configurations.forEach { buildConfiguration ->
                getter(buildConfiguration.id).forEach inner@{ build ->
                    LOG.debug("Processed $build")
                    val id = build.id.stringId.toLong()
                    if (id <= oldMaxId) return@inner
                    if (id > lastBuildId) lastBuildId = id

                    LOG.info("New build $build")
                    if (build.state == BuildState.RUNNING) {
                        myRunningBuilds[build] = BuildContext(project, buildConfiguration)
                    }

                    val startTime = build.startDateTime
                    if (startTime != null && startTime > lastTime) lastTime = startTime

                    buildHandle(BuildContext(project, buildConfiguration), build)
                }
            }
        }

        lastUpdate = lastTime.toInstant()
        LOG.debug("End check new builds")
    }

    private fun checkRunningBuilds() {
        LOG.debug("Start check running builds")
        myRunningBuilds.filter { (build, context) ->
            val newResult = teamCityInstance.build(build.id)
            if (newResult.state == BuildState.RUNNING) return@filter false
            LOG.info("Build $newResult is finish")
            buildHandle(context, newResult)
            true
        }.forEach { build, context -> myRunningBuilds.remove(build, context) }
        LOG.debug("End check running builds")
    }

    private fun buildHandle(buildContext: BuildContext, build: Build) {
        handlers.forEach {
            it.value(buildContext, build)
        }
    }

    private fun getRunningBuilds() {
        LOG.debug("Init running builds")
        checkNewBuilds {
            teamCityInstance.builds()
                .fromConfiguration(it)
                .onlyRunning()
                .all()
        }
    }

    private fun getNewBuilds(configurationId: BuildConfigurationId): Sequence<Build> =
        teamCityInstance.builds()
            .fromConfiguration(configurationId)
            .includeCanceled()
            .includeFailed()
            .includeRunning()
            .withAllBranches()
            .since(lastUpdate) // TODO: Replace with sinceBuild. Now this filter is not implemented in the teamcity-rest-client.
            .all()

    private suspend fun startCheckProjectStructure() {
        try {
            checkProjectStructure()
            getRunningBuilds()
            while (true) {
                checkProjectStructure()
                delay(checkProjectDelayMillis)
            }
        } catch (e: CancellationException) {
            LOG.debug("Cancel")
            return
        } catch (e: Exception) {
            LOG.warn("Error on check project structure", e)
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
        when (cascadeMode) {
            CascadeMode.RECURSIVELY -> fillProjectStructure(projects.flatMap(Project::childProjects), map)
            CascadeMode.ONLY_ROOT -> return
        }
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
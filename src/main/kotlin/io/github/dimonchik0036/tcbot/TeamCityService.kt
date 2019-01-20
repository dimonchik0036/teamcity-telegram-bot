package io.github.dimonchik0036.tcbot

import kotlinx.coroutines.*
import org.jetbrains.teamcity.rest.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

private val LOG = LoggerFactory.getLogger("team-city-service")

class TeamCityService(
    serverUrl: String,
    teamCityUser: TeamCityUser = Guest,
    private val cascadeMode: CascadeMode = CascadeMode.ONLY_ROOT,
    private val handlers: Map<String, TeamCityBuildHandler>,
    private var lastUpdate: Instant = Instant.now(),
    private val checkUpdatesDelayMillis: Long = 60_000,
    private val checkProjectDelayMillis: Long = 600_000,
    private val rootProjectsId: Set<ProjectId> = emptySet()
) {
    /**
     * Public API
     */
    @Volatile
    var runningBuilds: Set<TeamCityBuild> = emptySet()
        private set

    fun start() = runBlocking {
        LOG.info("Start")
        job = launch {
            launch { startCheckProjectStructure() }
            launch { startCheckUpdates() }
        }
    }

    fun stop() = job.cancel()

    //---------------------------------------------
    @Volatile
    private lateinit var job: Job

    private val teamCityInstance = teamCityUser.teamCityInstance(serverUrl)
    private val myRunningBuilds: HashSet<TeamCityBuild> = hashSetOf()

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

    private var projectStructure: Map<Project, List<BuildConfiguration>> = emptyMap()

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
        if (runningBuilds != myRunningBuilds) {
            runningBuilds = myRunningBuilds.toSet()
        }
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

                    val teamCityBuild = TeamCityBuild.fromBuild(build)
                    LOG.info("New build $build")
                    if (build.state == BuildState.RUNNING) {
                        myRunningBuilds += teamCityBuild
                    }

                    val startTime = build.startDateTime
                    if (startTime != null && startTime > lastTime) lastTime = startTime
                    buildHandle(teamCityBuild)
                }
            }
        }

        lastUpdate = lastTime.toInstant()
        LOG.debug("End check new builds")
    }

    private fun checkRunningBuilds() {
        LOG.debug("Start check running builds")
        runningBuilds.forEach {
            val build = teamCityInstance.build(BuildId(it.id))
            if (build.state == BuildState.RUNNING) {
                if (build.status != it.status) {
                    myRunningBuilds -= it
                    myRunningBuilds += TeamCityBuild.fromBuild(build)
                    LOG.info("Change status from ${it.status} to ${build.status} in $it")
                }
                return@forEach
            }
            LOG.info("Build $build is finish")
            buildHandle(TeamCityBuild.fromBuild(build))
            myRunningBuilds -= it
        }
        LOG.debug("End check running builds")
    }

    private fun buildHandle(build: TeamCityBuild) {
        handlers.forEach {
            LOG.debug("Handle `${it.key}` on $build")
            it.value(build)
        }
    }

    private fun initRunningBuilds() {
        LOG.debug("Init running builds")
        checkNewBuilds(this::getRunningBuilds)
    }

    private fun getRunningBuilds(configurationId: BuildConfigurationId): Sequence<Build> = teamCityInstance.builds()
        .fromConfiguration(configurationId)
        .onlyRunning()
        .all()

    private fun getNewBuilds(configurationId: BuildConfigurationId): Sequence<Build> = teamCityInstance.builds()
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
            initRunningBuilds()
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

        LOG.debug("End check project structure")
    }
}

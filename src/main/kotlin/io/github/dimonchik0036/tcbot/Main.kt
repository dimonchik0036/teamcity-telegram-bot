package io.github.dimonchik0036.tcbot

import io.github.dimonchik0036.tcbot.core.impl.SQLite
import org.jetbrains.teamcity.rest.ProjectId
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory

fun main(args: Array<String>) {
    val parameters = InputParameters(args)
    val db = SQLite(parameters.databasePath)
    val tc = TeamCityInstanceFactory.guestAuth("https://teamcity.jetbrains.com")
    db.addProject(tc.project(ProjectId("Kotlin")))
//    db.addProject(tc.rootProject())
//    tc.builds()
//        .fromConfiguration(BuildConfigurationId("Kotlin_dev_AggregateBranch"))
//        .withAllBranches()
//        .includeFailed()
//        .includeCanceled()
//        .limitResults(10)
//        .all().forEach {
//            println("${it.id} ${it.finishDateTime} ${it.buildNumber} ${it.branch.name} ${it.statusText}")
//        }
//    println(Instant.now())
}

package io.github.dimonchik0036.tcbot

import org.jetbrains.teamcity.rest.BuildState
import kotlin.test.*

class FilterTest {
    @Test
    fun `test default`() {
        val filter = Filter()
        listOf(
            createBuild(null, "id"),
            createBuild("name", "id"),
            createBuild("name", "other name")
        ).forEach {
            assertTrue(filter.matches(it))
        }
    }

    @Test
    fun `test branch filter`() {
        val filter = Filter()
        filter.setFilterByName("branch", Regex("na.*"))
        assertTrue(filter.matches(createBuild(null, "id")))
        assertTrue(filter.matches(createBuild("name", "id")))
        assertFalse(filter.matches(createBuild("not name", "other build con")))
    }

    @Test
    fun `test build configuration id filter`() {
        val filter = Filter()
        filter.setFilterByName("build_configuration", Regex("Kotlin_dev.*"))
        assertTrue(filter.matches(createBuild("name", "Kotlin_dev")))
        assertTrue(filter.matches(createBuild("name", "Kotlin_dev_AggregateBranch")))
        assertFalse(filter.matches(createBuild("name", "Kotlin")))
        assertFalse(filter.matches(createBuild("not name", "bt410")))
    }

    @Test
    fun `test random filter`() {
        val filter = Filter()
        filter.setFilterByName("build_configuration", Regex("Kotlin_dev.*"))
        filter.setFilterByName("branch", Regex("rr/[^/]*/[^/]*"))
        assertTrue(filter.matches(createBuild("rr/gradle/fix", "Kotlin_dev_AggregateBranch")))
        assertFalse(filter.matches(createBuild("rr/drop", "Kotlin_dev_AggregateBranch")))
        assertFalse(filter.matches(createBuild("rr/gradle/fix/now", "Kotlin_dev_AggregateBranch")))
        assertTrue(filter.matches(createBuild(null, "Kotlin_dev")))
        assertFalse(filter.matches(createBuild(null, "Kotlin")))
    }

    @Test
    fun `test setter`() {
        val filter = Filter()
        val regex = Regex(".*")
        assertTrue(filter.setFilterByName("branch", regex))
        assertTrue(filter.setFilterByName("build_configuration", regex))
        assertFalse(filter.setFilterByName("sss", regex))
    }

    @Test
    fun `test getter`() {
        val filter = Filter()
        assertNotNull(filter.getFilterByName("branch"))
        assertNotNull(filter.getFilterByName("build_configuration"))
        assertNull(filter.getFilterByName("sss"))
    }
}

private fun createBuild(branchName: String?, buildConfigurationId: String) = TeamCityBuild(
    id = "",
    name = "",
    buildConfigurationId = buildConfigurationId,
    branchName = branchName,
    url = "",
    number = "",
    lastAuthor = "",
    state = BuildState.RUNNING,
    status = null
)
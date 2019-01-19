package io.github.dimonchik0036.tcbot

class Filter(
    @Volatile
    var branchFilter: Regex = ALL,
    @Volatile
    var buildFilter: Regex = ALL
) {
    private fun matchesBranchName(name: String?): Boolean = name?.let { branchFilter.matches(name) } ?: true
    private fun matchesBuildConfigurationId(configuration: String) = buildFilter.matches(configuration)
    fun matches(build: TeamCityBuild) =
        matchesBranchName(build.branchName) && matchesBuildConfigurationId(build.buildConfigurationId)

    fun getFilterByName(name: String): Regex? = when (name) {
        "branch" -> branchFilter
        "build_configuration" -> buildFilter
        else -> null
    }

    fun setFilterByName(name: String, filter: Regex): Boolean {
        when (name) {
            "branch" -> branchFilter = filter
            "build_configuration" -> buildFilter = filter
            else -> return false
        }
        return true
    }

    companion object {
        private val ALL = Regex(".*")
        val FILTER_NAMES = listOf("branch", "build_configuration")
        fun isFilterName(name: String): Boolean = name in FILTER_NAMES
    }
}
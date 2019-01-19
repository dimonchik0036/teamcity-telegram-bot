package io.github.dimonchik0036.tcbot

class Filter(
    @Volatile
    var branchFilter: Regex? = null,
    @Volatile
    var buildFilter: Regex? = null
) {
    fun matchesBranchName(name: String): Boolean = branchFilter?.matches(name) ?: true
    fun matchesBuildConfigurationId(configuration: String) = buildFilter?.matches(configuration) ?: true
    fun getFilterByName(name: String): Pair<Boolean, Regex?> = when (name) {
        "branch" -> true to branchFilter
        "build_configuration" -> true to buildFilter
        else -> false to null
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
        val FILTER_NAMES = listOf("branch", "build_configuration")
        fun isFilterName(name: String): Boolean = name in FILTER_NAMES
    }
}
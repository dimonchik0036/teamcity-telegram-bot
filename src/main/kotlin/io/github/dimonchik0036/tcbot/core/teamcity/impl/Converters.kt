package io.github.dimonchik0036.tcbot.core.teamcity.impl

import io.requery.Converter
import org.jetbrains.teamcity.rest.*

class ProjectIdConverter : Converter<ProjectId, String> {
    override fun convertToMapped(type: Class<out ProjectId>?, value: String?): ProjectId? = value?.let { ProjectId(it) }
    override fun getPersistedType(): Class<String> = String::class.java
    override fun convertToPersisted(value: ProjectId?): String? = value?.stringId
    override fun getMappedType(): Class<ProjectId> = ProjectId::class.java
    override fun getPersistedSize(): Int? = null
}

class BuildIdConverter : Converter<BuildId, String> {
    override fun convertToMapped(type: Class<out BuildId>?, value: String?): BuildId? = value?.let { BuildId(it) }
    override fun getPersistedType(): Class<String> = String::class.java
    override fun convertToPersisted(value: BuildId?): String? = value?.stringId
    override fun getMappedType(): Class<BuildId> = BuildId::class.java
    override fun getPersistedSize(): Int? = null
}

class TestIdConverter : Converter<TestId, String> {
    override fun convertToMapped(type: Class<out TestId>?, value: String?): TestId? = value?.let { TestId(it) }
    override fun getPersistedType(): Class<String> = String::class.java
    override fun convertToPersisted(value: TestId?): String? = value?.stringId
    override fun getMappedType(): Class<TestId> = TestId::class.java
    override fun getPersistedSize(): Int? = null
}

class ChangeIdConverter : Converter<ChangeId, String> {
    override fun convertToMapped(type: Class<out ChangeId>?, value: String?): ChangeId? = value?.let { ChangeId(it) }
    override fun getPersistedType(): Class<String> = String::class.java
    override fun convertToPersisted(value: ChangeId?): String? = value?.stringId
    override fun getMappedType(): Class<ChangeId> = ChangeId::class.java
    override fun getPersistedSize(): Int? = null
}

class BuildConfigurationIdConverter : Converter<BuildConfigurationId, String> {
    override fun convertToMapped(type: Class<out BuildConfigurationId>?, value: String?): BuildConfigurationId? =
        value?.let { BuildConfigurationId(it) }

    override fun getPersistedType(): Class<String> = String::class.java
    override fun convertToPersisted(value: BuildConfigurationId?): String? = value?.stringId
    override fun getMappedType(): Class<BuildConfigurationId> = BuildConfigurationId::class.java
    override fun getPersistedSize(): Int? = null
}

class VcsRootIdConverter : Converter<VcsRootId, String> {
    override fun convertToMapped(type: Class<out VcsRootId>?, value: String?): VcsRootId? = value?.let { VcsRootId(it) }
    override fun getPersistedType(): Class<String> = String::class.java
    override fun convertToPersisted(value: VcsRootId?): String? = value?.stringId
    override fun getMappedType(): Class<VcsRootId> = VcsRootId::class.java
    override fun getPersistedSize(): Int? = null
}

class BuildProblemIdConverter : Converter<BuildProblemId, String> {
    override fun convertToMapped(type: Class<out BuildProblemId>?, value: String?): BuildProblemId? =
        value?.let { BuildProblemId(it) }

    override fun getPersistedType(): Class<String> = String::class.java
    override fun convertToPersisted(value: BuildProblemId?): String? = value?.stringId
    override fun getMappedType(): Class<BuildProblemId> = BuildProblemId::class.java
    override fun getPersistedSize(): Int? = null
}

class BuildProblemTypeConverter : Converter<BuildProblemType, String> {
    override fun convertToMapped(type: Class<out BuildProblemType>?, value: String?): BuildProblemType? =
        value?.let { BuildProblemType(it) }

    override fun getPersistedType(): Class<String> = String::class.java
    override fun convertToPersisted(value: BuildProblemType?): String? = value?.stringType
    override fun getMappedType(): Class<BuildProblemType> = BuildProblemType::class.java
    override fun getPersistedSize(): Int? = null
}

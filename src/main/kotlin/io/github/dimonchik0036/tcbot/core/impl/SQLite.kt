package io.github.dimonchik0036.tcbot.core.impl

import io.github.dimonchik0036.tcbot.core.DataType
import io.github.dimonchik0036.tcbot.core.Database
import io.github.dimonchik0036.tcbot.core.Models
import io.github.dimonchik0036.tcbot.core.teamcity.TeamCityProjectEntity
import io.requery.sql.*
import org.jetbrains.teamcity.rest.Project
import org.slf4j.LoggerFactory
import org.sqlite.JDBC
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.util.*

private val LOG = LoggerFactory.getLogger("sqlite-database")

class SQLite(path: String?, properties: Properties? = null) : Database {
    val config: Configuration
    val data: KotlinEntityDataStore<DataType>

    init {
        val dataSource = if (properties == null) SQLiteDataSource() else SQLiteDataSource(SQLiteConfig(properties))
        val db = path ?: "tcbot.db"
        dataSource.url = JDBC.PREFIX + db

        config = KotlinConfiguration(dataSource = dataSource, model = Models.DEFAULT)
        SchemaModifier(config).createTables(TableCreationMode.DROP_CREATE)

        data = KotlinEntityDataStore(config)

//        LOG.info("Starts initialization")
//        connection = dataSource.connection
//        LOG.debug("Connection is established")
//        if (!verifyTables()) {
//            LOG.info("Invalid tables")
//            initTables()
//        }
//        LOG.info("Initialized")
    }

    fun addProject(project: Project) {
        val filled = fill(project)
        data.invoke {
            insert(filled)
        }
        project.childProjects.forEach(::addProject)
    }

    private fun fill(project: Project): DataType {
        val p = TeamCityProjectEntity()
        p.archived = project.archived
        p.name = project.name
        p.id = project.id
        p.parentId = project.parentProjectId
        return p as DataType
    }

//    private fun verifyTables(): Boolean {
//        connection.createStatement().use {
//            val result = it.executeQuery("pragma table_info(\"$configTableName\")")
//            return result.next()
//                    && result.getString(NAME_COLUMN_INDEX) == configKeyColumnName
//                    && result.getString(TYPE_COLUMN_INDEX) == "integer"
//                    && result.next()
//                    && result.getString(NAME_COLUMN_INDEX) == configValueColumnName
//                    && result.getString(TYPE_COLUMN_INDEX) == "text"
//                    && !result.next()
//        }
//    }
//
//    private fun initTables() {
//        connection.createStatement().use {
//            LOG.debug("Drop table")
//            it.executeUpdate("drop table if exists $configTableName")
//
//            LOG.debug("Create table")
//            it.executeUpdate("create table $configTableName ($configKeyColumnName integer not null unique primary key, $configValueColumnName text not null)")
//        }
//
//        LOG.info("Tables initialized")
//    }

    companion object {
        private const val configTableName = "configurations"
        private const val configKeyColumnName = "key"
        private const val configValueColumnName = "value"

        private const val NAME_COLUMN_INDEX = 2
        private const val TYPE_COLUMN_INDEX = 3
    }
}

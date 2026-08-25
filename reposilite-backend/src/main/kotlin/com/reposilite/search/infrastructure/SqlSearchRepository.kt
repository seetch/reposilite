/*
 * Copyright (c) 2023 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.search.infrastructure

import com.reposilite.maven.api.GAV_MAX_LENGTH
import com.reposilite.maven.api.REPOSITORY_NAME_MAX_LENGTH
import com.reposilite.search.SearchRepository
import com.reposilite.search.api.SearchResult
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val MYSQL_FAMILY_VENDORS = setOf("mysql", "mariadb", "h2")

@Suppress("RemoveRedundantQualifierName")
internal class SqlSearchRepository(private val database: Database) : SearchRepository {

    object ArtifactIndexTable : IntIdTable("search_artifact_index") {
        val repository = varchar("repository", REPOSITORY_NAME_MAX_LENGTH).index("idx_search_artifact_index_repository")
        val gav = varchar("gav", GAV_MAX_LENGTH)

        init {
            uniqueIndex("uq_search_artifact_index_repository_gav", repository, gav)
        }
    }

    init {
        transaction(database) {
            SchemaUtils.create(ArtifactIndexTable)
            SchemaUtils.checkMappingConsistence(ArtifactIndexTable, withLogs = false).forEach { exec(it) }
        }
    }

    override fun index(repository: String, gav: String) {
        transaction(database) {
            val conflictKeys: Array<Column<*>> =
                if (database.vendor.lowercase() in MYSQL_FAMILY_VENDORS) emptyArray()
                else arrayOf(ArtifactIndexTable.repository, ArtifactIndexTable.gav)

            ArtifactIndexTable.upsert(*conflictKeys, onUpdate = {}) {
                it[ArtifactIndexTable.repository] = repository
                it[ArtifactIndexTable.gav] = gav
            }
        }
    }

    override fun unindex(repository: String, gav: String) {
        transaction(database) {
            ArtifactIndexTable.deleteWhere {
                (ArtifactIndexTable.repository eq repository) and
                    ((ArtifactIndexTable.gav eq gav) or (ArtifactIndexTable.gav like "$gav/%"))
            }
        }
    }

    override fun replaceRepositoryIndex(repository: String, entries: Collection<String>) {
        transaction(database) {
            ArtifactIndexTable.deleteWhere { ArtifactIndexTable.repository eq repository }

            entries.forEach { gav ->
                ArtifactIndexTable.insert {
                    it[ArtifactIndexTable.repository] = repository
                    it[ArtifactIndexTable.gav] = gav
                }
            }
        }
    }

    override fun findByPhrase(
        phrase: String,
        repository: String?,
        limit: Int,
        accessibleGavPrefixes: Set<String>?
    ): List<SearchResult> =
        transaction(database) {
            if (accessibleGavPrefixes?.isEmpty() == true) {
                return@transaction emptyList()
            }

            val restrictedPrefixes = accessibleGavPrefixes?.takeUnless { "" in it }
            val criteria = listOfNotNull(
                repository?.let { ArtifactIndexTable.repository eq it },
                phrase.takeIf(String::isNotEmpty)?.let { ArtifactIndexTable.gav.lowerCase() like it.toContainsPattern() },
                restrictedPrefixes?.let { prefixes ->
                    OrOp(prefixes.map { ArtifactIndexTable.gav.lowerCase() like (LikePattern.ofLiteral(it.lowercase()) + "%") })
                }
            ).let { if (it.isEmpty()) Op.TRUE else AndOp(it) }

            ArtifactIndexTable
                .selectAll()
                .where(criteria)
                .orderBy(ArtifactIndexTable.repository to SortOrder.ASC, ArtifactIndexTable.gav to SortOrder.ASC)
                .limit(limit)
                .map { SearchResult(it[ArtifactIndexTable.repository], it[ArtifactIndexTable.gav]) }
        }

}

private fun String.toContainsPattern(): LikePattern =
    LikePattern.ofLiteral(lowercase()).let { it.copy(pattern = "%${it.pattern}%") }

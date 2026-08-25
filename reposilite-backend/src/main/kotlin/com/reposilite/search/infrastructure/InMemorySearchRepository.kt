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

import com.reposilite.search.SearchRepository
import com.reposilite.search.api.SearchResult
import java.util.concurrent.ConcurrentHashMap

internal class InMemorySearchRepository : SearchRepository {

    private val index = ConcurrentHashMap<String, MutableSet<String>>()

    override fun index(repository: String, gav: String) {
        index.computeIfAbsent(repository) { ConcurrentHashMap.newKeySet() }.add(gav)
    }

    override fun unindex(repository: String, gav: String) {
        index[repository]?.removeIf { it == gav || it.startsWith("$gav/") }
    }

    override fun replaceRepositoryIndex(repository: String, entries: Collection<String>) {
        index[repository] = ConcurrentHashMap.newKeySet<String>().apply { addAll(entries) }
    }

    override fun findByPhrase(
        phrase: String,
        repository: String?,
        limit: Int,
        accessibleGavPrefixes: Set<String>?
    ): List<SearchResult> =
        index.asSequence()
            .filter { (repo, _) -> repository == null || repo == repository }
            .flatMap { (repo, gavs) -> gavs.asSequence().map { gav -> repo to gav } }
            .filter { (_, gav) -> phrase.isEmpty() || gav.contains(phrase, ignoreCase = true) }
            .filter { (_, gav) -> accessibleGavPrefixes == null || accessibleGavPrefixes.any { gav.startsWith(it, ignoreCase = true) } }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .take(limit)
            .map { (repo, gav) -> SearchResult(repo, gav) }
            .toList()

}

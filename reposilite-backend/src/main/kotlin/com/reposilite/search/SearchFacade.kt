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
package com.reposilite.search

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.RepositoryVisibility.PUBLIC
import com.reposilite.plugin.api.Facade
import com.reposilite.search.api.SearchResponse
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
import com.reposilite.shared.notFoundError
import com.reposilite.shared.unauthorizedError
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission.READ
import panda.std.Result
import panda.std.asSuccess

const val SEARCH_MAX_PAGE_SIZE = 100

class SearchFacade internal constructor(
    private val journalist: Journalist,
    private val searchRepository: SearchRepository,
    private val accessTokenFacade: AccessTokenFacade,
    private val mavenFacade: MavenFacade
) : Journalist, Facade {

    fun index(repository: String, gav: String) =
        searchRepository.index(repository, gav)

    fun unindex(repository: String, gav: String) =
        searchRepository.unindex(repository, gav)

    fun reindexRepository(repository: String, entries: Collection<String>) =
        searchRepository.replaceRepositoryIndex(repository, entries)

    /**
     * Search for artifacts within a single repository. Public repositories can be searched anonymously;
     * hidden/private repositories require an authenticated token, and results are filtered down to the
     * gav prefixes that token has read access to.
     */
    fun searchInRepository(
        repository: String,
        phrase: String,
        limit: Int = SEARCH_MAX_PAGE_SIZE,
        accessToken: AccessTokenIdentifier?
    ): Result<SearchResponse, ErrorResponse> {
        val targetRepository = mavenFacade.getRepository(repository)
            ?: return notFoundError("Repository $repository not found")

        return when {
            limit !in 1..SEARCH_MAX_PAGE_SIZE ->
                badRequestError("Requested invalid page size ($limit, expected 1..$SEARCH_MAX_PAGE_SIZE)")
            phrase.isBlank() ->
                SearchResponse(emptyList()).asSuccess()
            targetRepository.visibility == PUBLIC ->
                SearchResponse(searchRepository.findByPhrase(phrase, repository, limit, null)).asSuccess()
            accessToken == null ->
                unauthorizedError("You need to provide credentials to search this repository")
            else ->
                SearchResponse(
                    searchRepository.findByPhrase(phrase, repository, limit, getAccessibleGavPrefixes(accessToken, repository))
                ).asSuccess()
        }
    }

    /**
     * Search for artifacts across all repositories at once. Requires manager permission,
     * since results cannot be filtered down to a single token's accessible gav prefixes.
     */
    fun searchAllRepositories(
        phrase: String,
        limit: Int = SEARCH_MAX_PAGE_SIZE
    ): Result<SearchResponse, ErrorResponse> =
        when {
            limit !in 1..SEARCH_MAX_PAGE_SIZE ->
                badRequestError("Requested invalid page size ($limit, expected 1..$SEARCH_MAX_PAGE_SIZE)")
            phrase.isBlank() ->
                SearchResponse(emptyList()).asSuccess()
            else ->
                SearchResponse(searchRepository.findByPhrase(phrase, null, limit, null)).asSuccess()
        }

    /**
     * Resolves the set of gav prefixes (within the given repository) the token has read access to.
     * Returns null for a manager token (unrestricted). An empty set means the token has no read access
     * to anything in this repository, in which case the search naturally returns no results.
     */
    private fun getAccessibleGavPrefixes(accessToken: AccessTokenIdentifier, repository: String): Set<String>? {
        if (accessTokenFacade.hasPermission(accessToken, MANAGER)) {
            return null
        }

        return accessTokenFacade.getRoutes(accessToken)
            .asSequence()
            .filter { it.permission == READ }
            .mapNotNull { it.toGavPrefix(repository) }
            .toSet()
    }

    private fun Route.toGavPrefix(repository: String): String? {
        val repositoryRoot = "/$repository"
        return when {
            repositoryRoot.startsWith(path, ignoreCase = true) -> ""
            path.startsWith("$repositoryRoot/", ignoreCase = true) -> path.substring(repositoryRoot.length + 1)
            else -> null
        }
    }

    override fun getLogger(): Logger =
        journalist.logger

}

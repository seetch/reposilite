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

import com.reposilite.search.SEARCH_MAX_PAGE_SIZE
import com.reposilite.search.SearchFacade
import com.reposilite.search.api.SearchResponse
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.GET
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse

internal class SearchEndpoint(private val searchFacade: SearchFacade) : ReposiliteRoutes() {

    @OpenApi(
        tags = ["Search"],
        path = "/api/search",
        methods = [HttpMethod.GET],
        queryParams = [
            OpenApiParam(name = "phrase", description = "Phrase to search for in artifact coordinates", required = true),
            OpenApiParam(name = "repository", description = "Repository to search in. If omitted, all repositories are searched (requires manager permission)", required = false),
            OpenApiParam(name = "limit", description = "Amount of results to find (Maximum: $SEARCH_MAX_PAGE_SIZE)", required = false)
        ],
        responses = [
            OpenApiResponse("200", content = [ OpenApiContent(from = SearchResponse::class) ], description = "Artifacts matching the given phrase"),
            OpenApiResponse("400", content = [ OpenApiContent(from = ErrorResponse::class) ], description = "When invalid page size is used"),
            OpenApiResponse("401", content = [ OpenApiContent(from = ErrorResponse::class) ], description = "When a hidden/private repository is searched without credentials"),
            OpenApiResponse("403", content = [ OpenApiContent(from = ErrorResponse::class) ], description = "When a global search is requested by a non-manager token")
        ]
    )
    private val search = ReposiliteRoute<SearchResponse>("/api/search", GET) {
        val phrase = ctx.queryParam("phrase").orEmpty()
        val repository = ctx.queryParam("repository")?.takeIf(String::isNotBlank)
        val rawLimit = ctx.queryParam("limit")
        val limit = rawLimit?.toIntOrNull()

        when {
            rawLimit != null && limit == null ->
                response = badRequestError("Requested invalid page size ($rawLimit, expected 1..$SEARCH_MAX_PAGE_SIZE)")
            repository == null ->
                managerOnly {
                    response = searchFacade.searchAllRepositories(phrase, limit ?: SEARCH_MAX_PAGE_SIZE)
                }
            else ->
                accessed {
                    response = searchFacade.searchInRepository(repository, phrase, limit ?: SEARCH_MAX_PAGE_SIZE, this?.identifier)
                }
        }
    }

    override val routes = routes(search)

}

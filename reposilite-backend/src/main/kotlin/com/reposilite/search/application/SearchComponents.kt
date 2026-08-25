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

package com.reposilite.search.application

import com.reposilite.journalist.Journalist
import com.reposilite.maven.MavenFacade
import com.reposilite.plugin.api.PluginComponents
import com.reposilite.search.SearchFacade
import com.reposilite.search.SearchRepository
import com.reposilite.search.infrastructure.InMemorySearchRepository
import com.reposilite.search.infrastructure.SqlSearchRepository
import com.reposilite.token.AccessTokenFacade
import org.jetbrains.exposed.v1.jdbc.Database

class SearchComponents(
    private val journalist: Journalist,
    private val database: Database?,
    private val accessTokenFacade: AccessTokenFacade,
    private val mavenFacade: MavenFacade
) : PluginComponents {

    private fun searchRepository(): SearchRepository =
        when (database) {
            null -> InMemorySearchRepository()
            else -> SqlSearchRepository(database)
        }

    fun searchFacade(searchRepository: SearchRepository = searchRepository()): SearchFacade =
        SearchFacade(
            journalist = journalist,
            searchRepository = searchRepository,
            accessTokenFacade = accessTokenFacade,
            mavenFacade = mavenFacade
        )

}

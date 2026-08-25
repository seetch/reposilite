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

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.DeleteEvent
import com.reposilite.maven.api.DeployEvent
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.reposilite
import com.reposilite.search.SearchFacade
import com.reposilite.search.infrastructure.SearchEndpoint
import com.reposilite.search.walkGavs
import com.reposilite.token.AccessTokenFacade
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "search", dependencies = ["access-token", "maven"])
internal class SearchPlugin : ReposilitePlugin() {

    override fun initialize(): SearchFacade {
        val searchFacade = SearchComponents(
            journalist = this,
            database = reposilite().database,
            accessTokenFacade = facade<AccessTokenFacade>(),
            mavenFacade = facade<MavenFacade>()
        ).searchFacade()

        event { event: RoutingSetupEvent ->
            event.registerRoutes(SearchEndpoint(searchFacade))
        }

        event { event: DeployEvent ->
            searchFacade.index(event.repository.name, event.gav.toString())
        }

        event { event: DeleteEvent ->
            searchFacade.unindex(event.repository.name, event.gav.toString())
        }

        event { _: ReposiliteInitializeEvent ->
            val mavenFacade = facade<MavenFacade>()

            reposilite().ioService.execute {
                mavenFacade.getRepositories().forEach { repository ->
                    searchFacade.reindexRepository(repository.name, repository.walkGavs())
                }
                logger.info("Search | Artifact index has been built (${mavenFacade.getRepositories().size} repositories)")
            }
        }

        return searchFacade
    }

}

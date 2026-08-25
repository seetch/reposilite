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

import com.reposilite.maven.Repository
import com.reposilite.storage.api.FileType.DIRECTORY
import com.reposilite.storage.api.Location

private val IGNORED_SUFFIXES = listOf(".md5", ".sha1", ".sha256", ".sha512", ".asc")

/**
 * Walks through the whole repository storage and collects gavs of all indexable files (skips pure checksum/signature files).
 * Used to (re)build the search index from scratch, e.g. on startup or when migrating from a version without search support.
 */
internal fun Repository.walkGavs(): List<String> {
    val result = mutableListOf<String>()
    val directories = ArrayDeque<Location>()
    directories.add(Location.empty())

    while (directories.isNotEmpty()) {
        val directory = directories.removeFirst()

        storageProvider.getFiles(directory).orNull()?.forEach { child ->
            storageProvider.getFileDetails(child).orNull()?.let { details ->
                when {
                    details.type == DIRECTORY -> directories.add(child)
                    IGNORED_SUFFIXES.none { child.toString().endsWith(it) } -> result.add(child.toString())
                }
            }
        }
    }

    return result
}

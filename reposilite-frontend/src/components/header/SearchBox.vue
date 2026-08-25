<!--
  - Copyright (c) 2023 dzikoysk
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { useSession } from '../../store/session'
import useQualifier from '../../store/qualifier'
import useSearch from '../../store/search'
import SearchIcon from '../icons/SearchIcon.vue'
import CloseIcon from '../icons/CloseIcon.vue'

const { isManager } = useSession()
const { qualifier, redirectTo } = useQualifier()
const { query, results, loading, error, search, clear } = useSearch()

const open = ref(false)
const input = ref()

const currentRepository = computed(() => qualifier.path?.split('/')[0] || '')

// The shortcut hint should match what the visitor's OS actually expects — ⌘ means
// nothing (and looks broken glued to a letter) outside of macOS.
const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform ?? navigator.userAgent)
const shortcutHint = isMac ? '⌘ K' : 'Ctrl K'

// Repository-scoped search works for everyone (public repositories are searchable anonymously,
// hidden/private ones are enforced server-side). Only the cross-repository search needs a manager token.
const disabledReason = computed(() => {
  if (currentRepository.value || isManager.value) return ''
  return 'Open a repository to search'
})

const openSearch = () => {
  if (disabledReason.value) return
  open.value = true
  nextTick(() => input.value?.focus())
}

const closeSearch = () => {
  open.value = false
  clear()
}

const runSearch = useDebounceFn((phrase) => {
  search(phrase, currentRepository.value || undefined)
}, 250)

const onInput = (event) => {
  query.value = event.target.value
  runSearch(query.value)
}

watch(currentRepository, () => {
  if (query.value.trim().length > 0) {
    runSearch(query.value)
  }
})

const selectResult = (result) => {
  const parent = result.gav.includes('/') ? result.gav.substring(0, result.gav.lastIndexOf('/')) : ''
  redirectTo(`/${result.repository}${parent ? '/' + parent : ''}`)
  closeSearch()
}

const onKeyDown = (event) => {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    open.value ? closeSearch() : openSearch()
  } else if (event.key === 'Escape' && open.value) {
    closeSearch()
  }
}

onMounted(() => window.addEventListener('keydown', onKeyDown))
onUnmounted(() => window.removeEventListener('keydown', onKeyDown))
</script>

<template>
  <div>
  <button
    type="button"
    :disabled="!!disabledReason"
    class="group flex h-9 w-full items-center gap-2 rounded-lg border-1 border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 px-3 text-left text-gray-400 transition-colors duration-200 hover:border-accent-400 dark:hover:border-accent-500 disabled:(opacity-60 cursor-not-allowed hover:border-gray-200 dark:hover:border-gray-800)"
    @click="openSearch"
  >
    <SearchIcon class="shrink-0" aria-hidden="true" />
    <span class="flex-1 truncate text-sm">{{ disabledReason || 'Search artifacts…' }}</span>
    <kbd v-if="!disabledReason" class="shrink-0 hidden sm:inline text-xs px-1.5 py-0.5 rounded border-1 border-gray-200 dark:border-gray-700 text-gray-400">{{ shortcutHint }}</kbd>
  </button>

  <div
    v-if="open"
    class="fixed inset-0 z-50 flex items-start justify-center bg-black/60 backdrop-blur-sm px-4 pt-[10vh]"
    @mousedown.self="closeSearch"
  >
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Search artifacts"
      class="w-full max-w-xl max-h-[70vh] flex flex-col overflow-hidden surface-card shadow-surface-lg"
    >
      <div class="flex items-center gap-3 border-b-1 border-gray-150 dark:border-gray-800 px-4">
        <SearchIcon class="shrink-0 text-gray-400" aria-hidden="true" />
        <input
          ref="input"
          type="text"
          :value="query"
          placeholder="Search artifacts…"
          class="h-11 flex-1 bg-transparent text-sm text-gray-800 dark:text-gray-100 placeholder-gray-400 focus:outline-none"
          aria-label="Search artifacts"
          @input="onInput"
        >
        <button
          type="button"
          aria-label="Close search"
          class="icon-btn shrink-0 text-gray-400 hover:(bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-200) transition-colors"
          @click="closeSearch"
        >
          <CloseIcon aria-hidden="true" />
        </button>
      </div>

      <div class="min-h-0 flex-1 overflow-y-auto p-2">
        <p v-if="loading" class="px-3 py-8 text-center text-sm text-gray-400">
          Searching…
        </p>
        <p v-else-if="error" class="px-3 py-8 text-center text-sm text-gray-400">
          {{ error }}
        </p>
        <p v-else-if="query.trim().length === 0" class="px-3 py-8 text-center text-sm text-gray-400">
          {{ currentRepository ? `Searching in ${currentRepository}` : 'Type to search across all repositories' }}
        </p>
        <template v-else>
          <button
            v-for="result in results"
            :key="`${result.repository}/${result.gav}`"
            type="button"
            class="block w-full rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-gray-100 dark:hover:bg-gray-800"
            @click="selectResult(result)"
          >
            <span class="flex items-center gap-2 min-w-0">
              <span class="min-w-0 flex-1 truncate text-sm font-medium text-gray-800 dark:text-gray-100">{{ result.gav }}</span>
              <span class="shrink-0 text-xs text-gray-400">{{ result.repository }}</span>
            </span>
          </button>
          <p v-if="results.length === 0" class="px-3 py-8 text-center text-sm text-gray-400">
            No matches. Try different words.
          </p>
        </template>
      </div>
    </div>
  </div>
  </div>
</template>

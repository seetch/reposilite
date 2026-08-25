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
import { nextTick, ref, watch, watchEffect } from 'vue'
import { onClickOutside, useClipboard } from '@vueuse/core'
import { useSession } from '../../store/session'
import useRepository from '../../store/maven/repository'
import useMetadata from '../../store/maven/metadata'
import CopyIcon from '../icons/CopyIcon.vue'
import CopiedIcon from '../icons/CopiedIcon.vue'
import AdjustmentsIcon from '../icons/AdjustmentsIcon.vue'
import CardMenu from './CardMenu.vue'
import RepositorySnippet from "./RepositorySnippet.vue"
import ArtifactSnippet from "./ArtifactSnippet.vue"

const props = defineProps({
  qualifier: {
    type: Object,
    required: true
  }
})

const title = ref('')
const configurations = [
  { name: 'Maven', lang: 'xml' },
  { name: 'Gradle Kotlin', lang: 'kotlin' },
  { name: 'Gradle Groovy', lang: 'groovy' },
  { name: 'SBT', lang: 'scala' }
]
const data = ref({})
const loading = ref(false)
const { createRepositories } = useRepository()
const { parseMetadata } = useMetadata()
const { client } = useSession()
const { copy: copyText, isSupported: isCopySupported } = useClipboard()

const displayRepository = () => {
  title.value = 'Repository details'
  data.value = createRepositories(props.qualifier)
}

const displayArtifact = (metadataSource, version) => {
  title.value = 'Artifact details'
  const { groupId, artifactId, versions } = parseMetadata(metadataSource)
  const latestVersion = versions[version ? versions.indexOf(version) : versions.length - 1]
  data.value = { type: 'artifact', groupId, artifactId, version: latestVersion }
}

watchEffect(() => {
  // 1. Check if gave enough length to be an artifact
  //   1.1 check if gav is an artifact (fetch and process metadata file)
  // 2. If not display repository credentials if at least GAV has one element
  // 3. If GAV is empty display generic snippet to repository like domain.com/{repository}
  const qualifier = props.qualifier.path
  const elements = qualifier.split('/')

  const isStale = () => qualifier !== props.qualifier.path

  if (elements.length === 1 && elements[0] == '') {
    loading.value = false
    displayRepository()
    return
  }

  loading.value = true

  client.value.maven.content(`${qualifier}/maven-metadata.xml`)
    .then(response => {
      if (isStale()) return
      displayArtifact(response.data)
      loading.value = false
    })
    .catch(() => {
      client.value.maven.content(`${qualifier.substring(0, qualifier.indexOf(elements[elements.length-1])-1)}/maven-metadata.xml`)
        .then(response => {
          if (isStale()) return
          displayArtifact(response.data, elements[elements.length-1])
          loading.value = false
        })
        .catch(error => {
          if (isStale()) return
          if (error.response?.status !== 404 && error.response?.status !== 403) {
            console.log(error)
          }
          displayRepository()
          loading.value = false
        })
    })
})

const scopeOptions = ['compile', 'provided', 'runtime', 'test', 'system', 'import']
const scope = ref(localStorage.getItem('artifact-scope') || 'provided')
watchEffect(() => localStorage.setItem('artifact-scope', scope.value))

// Positioned in JS (not pure CSS) and teleported to <body> so it always renders fully
// on screen — flips above/below and clamps horizontally based on real viewport space,
// regardless of where the card ends up scrolled to.
const scopeButtonRef = ref()
const scopeMenuRef = ref()
const scopeMenuOpen = ref(false)
const scopeMenuPosition = ref({})

const positionScopeMenu = () => {
  const buttonRect = scopeButtonRef.value.getBoundingClientRect()
  const menuWidth = 144
  const estimatedMenuHeight = scopeOptions.length * 34 + 8
  const openUpward = window.innerHeight - buttonRect.bottom < estimatedMenuHeight && buttonRect.top > estimatedMenuHeight
  const left = Math.min(Math.max(8, buttonRect.right - menuWidth), window.innerWidth - menuWidth - 8)

  scopeMenuPosition.value = openUpward
    ? { left: `${left}px`, bottom: `${window.innerHeight - buttonRect.top + 6}px` }
    : { left: `${left}px`, top: `${buttonRect.bottom + 6}px` }
}

const openScopeMenu = async () => {
  scopeMenuOpen.value = true
  await nextTick()
  positionScopeMenu()
}

const closeScopeMenu = () => {
  scopeMenuOpen.value = false
}

const selectScope = (option) => {
  scope.value = option
  closeScopeMenu()
}

onClickOutside(scopeMenuRef, closeScopeMenu, { ignore: [scopeButtonRef] })

const selectedTab = ref()
const transitionName = ref('slide-right')

watch(selectedTab, (to, from) => {
  const toIndex = configurations.findIndex(entry => entry.name === to)
  const fromIndex = configurations.findIndex(entry => entry.name === from)
  transitionName.value = toIndex - fromIndex < 0 ? 'slide-left' : 'slide-right'
})

const snippetRef = ref()
const copied = ref(false)

const copy = async () => {
  if (copied.value) return
  let snippet = snippetRef.value[0].content.trim()
  await copyText(snippet)
  copied.value = true
  setTimeout(() => {
    copied.value = false
  }, 2000)
}

const selectTab = (tab) =>
  selectedTab.value = tab
</script>

<template>
  <section
    class="surface-card p-7"
    :aria-busy="loading"
    aria-labelledby="snippet-card-title"
  >
    <div class="flex flex-row justify-between">
      <h2 id="snippet-card-title" class="font-semibold tracking-tight flex items-center w-full">
        <span v-if="loading" class="h-4 w-36 rounded bg-gray-200 dark:bg-gray-700 skeleton-bars" aria-hidden="true" />
        <template v-else>{{title}}</template>
      </h2>
    </div>

    <CardMenu
      :configurations="configurations"
      @selectTab="selectTab"
    />

    <hr class="dark:border-gray-800 <sm:(hidden)">

    <div class="mt-6">
      <transition :name="transitionName" mode="out-in">
        <div :key="selectedTab" class="relative">
          <div class="absolute top-2 right-2 z-10 flex items-center gap-1.5">
            <div v-if="data.type === 'artifact' && !loading">
              <button
                ref="scopeButtonRef"
                type="button"
                class="icon-btn bg-white dark:bg-gray-900 border-1 border-gray-200 dark:border-gray-700 shadow-surface text-gray-500 dark:text-gray-400 hover:(text-gray-800 dark:text-gray-100 border-accent-400 dark:border-accent-500) transition-colors duration-200"
                :aria-label="`Scope: ${scope}. Change scope`"
                :title="`Scope: ${scope}`"
                aria-haspopup="listbox"
                :aria-expanded="scopeMenuOpen"
                @click="scopeMenuOpen ? closeScopeMenu() : openScopeMenu()"
              >
                <AdjustmentsIcon aria-hidden="true" />
              </button>
              <Teleport to="body">
                <div
                  v-if="scopeMenuOpen"
                  ref="scopeMenuRef"
                  class="fixed w-36 surface-card shadow-surface-lg py-1 z-50"
                  :style="scopeMenuPosition"
                  role="listbox"
                  aria-label="Scope"
                >
                  <button
                    v-for="option in scopeOptions"
                    :key="option"
                    type="button"
                    role="option"
                    :aria-selected="option === scope"
                    class="block w-full text-left px-3 py-1.5 text-sm hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                    :class="option === scope ? 'text-accent-600 dark:text-accent-400 font-medium' : 'text-gray-600 dark:text-gray-300'"
                    @click="selectScope(option)"
                  >
                    {{ option }}
                  </button>
                </div>
              </Teleport>
            </div>
            <button
              v-if="isCopySupported && !loading"
              type="button"
              class="icon-btn bg-white dark:bg-gray-900 border-1 border-gray-200 dark:border-gray-700 shadow-surface text-gray-500 dark:text-gray-400 hover:(text-gray-800 dark:text-gray-100 border-accent-400 dark:border-accent-500) transition-colors duration-200"
              :aria-label="copied ? 'Snippet copied' : 'Copy snippet'"
              :title="copied ? 'Snippet copied' : 'Copy snippet'"
              @click="copy"
            >
              <span v-if="copied" class="sr-only" role="status">Copied</span>
              <CopiedIcon v-if="copied" class="text-green-500" aria-hidden="true" />
              <CopyIcon v-else aria-hidden="true" />
            </button>
          </div>
          <div class="card-editor font-mono text-xs min-h-29 relative py-3 px-4 rounded-lg bg-gray-100 dark:bg-gray-800">
            <div v-if="loading" class="skeleton-bars space-y-2.5 pt-1" aria-hidden="true">
              <div class="h-3 rounded bg-gray-200 dark:bg-gray-700" style="width: 80%" />
              <div class="h-3 rounded bg-gray-200 dark:bg-gray-700" style="width: 55%" />
              <div class="h-3 rounded bg-gray-200 dark:bg-gray-700" style="width: 68%" />
              <div class="h-3 rounded bg-gray-200 dark:bg-gray-700" style="width: 40%" />
            </div>
            <template v-else>
              <template v-for="entry in configurations" :key="entry.name">
                <template v-if="entry.name === selectedTab">
                  <RepositorySnippet
                      v-if="data.type === 'repository'"
                      ref="snippetRef"
                      :configuration="entry"
                      :data="data"
                  />
                  <ArtifactSnippet
                      v-else-if="data.type === 'artifact'"
                      ref="snippetRef"
                      :configuration="entry"
                      :data="data"
                      :scope="scope"
                  />
                </template>
              </template>
            </template>
          </div>
        </div>
      </transition>
    </div>
  </section>
</template>

<style>
#card-menu button {
  border-top-left-radius: 10%;
  border-top-right-radius: 10%;
}
#card-menu button:hover {
  @apply bg-gray-100 dark:bg-gray-800;
  transition: background-color 0.5s;
}

.slide-right-enter-active,
.slide-right-leave-active,
.slide-left-enter-active,
.slide-left-leave-active {
  transition: opacity .1s ease, transform .1s ease;
}

.slide-right-leave-to,
.slide-left-enter-from {
  opacity: 0;
  transform: translateX(60px);
}

.slide-right-enter-from,
.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-60px);
}

::-webkit-scrollbar {
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background-color: rgba(155, 155, 155, 0.4);
  border-radius: 20px;
  border: transparent;
  margin-top: 10px;
}

.card-editor > pre {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>

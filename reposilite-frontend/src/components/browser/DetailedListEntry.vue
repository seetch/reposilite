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
import prettyBytes from 'pretty-bytes'
import {createURL} from "../../store/client"
import {useSession} from '../../store/session'
import usePlaceholders from '../../store/placeholders'
import EyeIcon from '../icons/EyeIcon.vue'
import JavaDocsIcon from "../icons/JavaDocsIcon.vue"
import TrashIcon from '../icons/TrashIcon.vue'
import {property} from '../../helpers/vue-extensions'

const props = defineProps({
  file: property(Object, true),
  qualifier: property(Object, true),
  url: property(String, false),
  openDeleteEntryModal: property(Function, true)
})

const { hasPermissionTo } = useSession()
const { javadocSuffixes } = usePlaceholders()

const humanReadableMimeTypes = ['application/xml', 'text/plain', 'text/xml', 'text/markdown', 'application/json']
const isHumanReadable = humanReadableMimeTypes.some(type => props.file?.contentType == type)

const openUrl = (url) =>
    window.open(url, '_blank')

const isJavaDocsAvailable = () => javadocSuffixes.some(suffix => props.file.name.endsWith(suffix)) && getJavaDocsUrl() != null
const getJavaDocsUrl = () => {
  const qualifier = props.qualifier.path
  const elements = qualifier.split('/')

  if (elements.length < 2 || elements[1] === '') {
      return null
  }

  return createURL(`/javadoc/${qualifier}`)
}
</script>

<template>
  <div class="browser-entry default-entry pointer-events-none">
    <div class="flex flex-row items-center max-w-full">
      <div v-if="file.type == 'DIRECTORY'" class="default-icon">⚫</div>
      <div v-else class="default-icon">⚪</div>
      <div class="default-filename">{{file.name}}</div>
    </div>
    <div class="entry-details flex flex-1 items-center justify-end gap-3 pr-6">
      <div class="entry-menu relative z-10 flex flex-row items-center justify-end gap-1 min-h-9 opacity-0 pointer-events-none">
        <button
          v-if="file.hasOwnProperty('contentLength') && isHumanReadable"
          type="button"
          :title="`Click to view ${file.name} file content in a new tab`"
          :aria-label="`View ${file.name} file content in a new tab`"
          class="icon-btn text-accent-500 dark:text-accent-400 hover:(bg-gray-100 dark:bg-gray-900)"
          @click.left.prevent="openUrl(url)"
          v-on:click.stop
        >
          <EyeIcon aria-hidden="true" />
        </button>
        <button
          v-if="isJavaDocsAvailable()"
          type="button"
          :title="`Click to view ${file.name} javadocs in a new tab`"
          :aria-label="`View ${file.name} Javadocs in a new tab`"
          class="icon-btn text-accent-500 dark:text-accent-400 hover:(bg-gray-100 dark:bg-gray-900)"
          @click.left.prevent="openUrl(getJavaDocsUrl())"
          v-on:click.stop
        >
          <JavaDocsIcon aria-hidden="true" />
        </button>
        <button
          v-if="qualifier.path.length > 1 && hasPermissionTo(`/${qualifier.path}`, 'route:write')"
          type="button"
          class="icon-btn text-accent-500 dark:text-accent-400 hover:(bg-gray-100 dark:bg-gray-900)"
          :aria-label="`Delete ${file.name}`"
          :title="`Delete ${file.name}`"
          @click.left.prevent="openDeleteEntryModal(file.name)"
          v-on:click.stop
        >
          <TrashIcon aria-hidden="true" />
        </button>
      </div>
      <div v-if="file.hasOwnProperty('contentLength')">
        {{ prettyBytes(file.contentLength) }}
      </div>
    </div>
  </div>
</template>

<style>
#browser-list li:hover .entry-menu,
#browser-list li:focus-within .entry-menu {
  opacity: 1;
  pointer-events: auto;
}

#browser-list li:hover .default-entry {
  @apply transition-color bg-gray-200 duration-500;
}
.dark #browser-list li:hover .default-entry {
  @apply bg-gray-800;
}

.default-entry {
  @apply flex flex-row items-center justify-between mb-2.5 py-2 rounded-lg default-button;
}

.default-icon {
  @apply text-sm px-6 <sm:px-3;
}

.default-filename {
  @apply font-semibold;
  overflow-wrap: anywhere;
}
</style>

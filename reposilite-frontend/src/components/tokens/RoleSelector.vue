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
import { property } from '../../helpers/vue-extensions'

const props = defineProps({
  roles: property(Object, true),
  role: property(String, true),
  repositories: property(Object, true), // [{ name, path }]
  repositoryPaths: property(Object, true),
  needsRepositoryPicker: property(Function, true)
})

const emit = defineEmits(['update:role', 'toggleRepository'])
</script>

<template>
  <div
    class="inline-flex overflow-hidden rounded-lg border border-gray-300 dark:border-gray-700"
    role="group"
    aria-label="Role"
  >
    <button
      v-for="option in roles"
      :key="option.id"
      type="button"
      :title="option.description"
      class="h-8 border-r border-gray-300 bg-white px-3 text-xs text-gray-600 last:border-r-0 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
      :class="{ '!bg-accent-600 !text-white': role === option.id }"
      :aria-pressed="role === option.id"
      @click="emit('update:role', option.id)"
    >
      {{ option.label }}
    </button>
  </div>

  <div
    v-if="needsRepositoryPicker(role)"
    class="flex flex-wrap items-center gap-1.5"
    role="group"
    aria-label="Repositories this role applies to"
  >
    <template v-if="repositories.length">
      <button
        v-for="repository in repositories"
        :key="repository.path"
        type="button"
        class="h-8 rounded-lg border border-gray-300 bg-white px-2.5 text-xs text-gray-600 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-400"
        :class="{ '!border-accent-500 !bg-accent-600 !text-white dark:!border-accent-400': repositoryPaths.includes(repository.path) }"
        :aria-pressed="repositoryPaths.includes(repository.path)"
        @click="emit('toggleRepository', repository.path)"
      >
        {{ repository.name }}
      </button>
    </template>
    <span
      v-else
      class="text-xs text-gray-400"
    >No repositories found</span>
  </div>
</template>

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
import usePlaceholders from '../../store/placeholders'
import HeaderHero from './HeaderHero.vue'
import MenuPanel from './MenuPanel.vue'
import SearchBox from './SearchBox.vue'

defineProps({
  logoClickCallback: {
    type: Function,
    required: false
  }
})

const { title } = usePlaceholders()
</script>

<template>
  <div>
    <header class="sticky top-0 z-40 h-14 bg-gray-100 dark:bg-black dark:text-white border-b-1 border-gray-150 dark:border-gray-900">
      <div class="container mx-auto h-full grid grid-cols-[1fr_auto_1fr] items-center gap-3">
        <p class="min-w-0 truncate text-sm font-semibold tracking-tight">
          <router-link :to="'/'" @click="logoClickCallback">
            {{ title }}
          </router-link>
        </p>
        <div class="flex justify-center <sm:hidden">
          <SearchBox class="w-64 lg:w-96" />
        </div>
        <MenuPanel class="col-start-3 justify-self-end" />
      </div>
    </header>
    <div class="hidden <sm:block border-b-1 border-gray-150 dark:border-gray-900 px-3 py-2">
      <SearchBox />
    </div>
    <HeaderHero class="pt-8 pb-10 <sm:pb-4" />
  </div>
</template>

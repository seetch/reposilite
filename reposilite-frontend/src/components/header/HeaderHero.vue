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
import GlobeIcon from '../icons/GlobeIcon.vue'
import usePlaceholders from '../../store/placeholders'

const { description, organizationWebsite, organizationLogo } = usePlaceholders()
</script>

<template>
  <section class="bg-gray-100 dark:bg-black" aria-label="Organization information">
    <div class="container mx-auto flex flex-row <sm:(flex-col items-center)">
      <div class="w-35 <sm:mb-2">
        <img
          class="border-2 rounded-full border-gray-150 dark:border-gray-800"
          :src="organizationLogo"
          alt="Organization logo"
        >
      </div>
      <div class="flex flex-col justify-center px-10 <sm:px-0">
        <div class="<sm:text-center" v-html="description"></div>
        <div class="flex flex-row items-center py-2 <sm:justify-center">
          <GlobeIcon class="text-gray-400" aria-hidden="true" />
          <a
            class="px-3 text-gray-500 dark:text-gray-400 hover:text-accent-600 dark:hover:text-accent-400 transition-colors"
            :href="organizationWebsite"
            target="_blank"
            rel="noopener noreferrer"
          >{{organizationWebsite}}</a>
        </div>
      </div>
    </div>
  </section>
</template>

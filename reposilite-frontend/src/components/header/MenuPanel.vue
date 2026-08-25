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
import { useSession } from '../../store/session'
import MenuButton from './MenuButton.vue'
import LoginModal from './LoginModal.vue'
import MoonIcon from '../icons/MoonIcon.vue'
import SunIcon from '../icons/SunIcon.vue'
import LogoutIcon from '../icons/LogoutIcon.vue'
import useTheme from "../../store/theme"

const { theme, changeTheme } = useTheme()
const { token, isLogged, logout } = useSession()

// Two states only: click always switches to the opposite of what's showing right now.
// The very first render can still follow the OS preference (theme.mode starts as 'auto'
// until the user picks explicitly), but the control itself never exposes a third state.
const toggleTheme = () =>
  changeTheme(theme.isDark ? 'light' : 'dark')
</script>

<template>
  <nav
    class="flex flex-row items-center gap-2 flex-none"
    aria-label="Account and display settings"
  >
    <div v-if="isLogged" class="px-2 text-sm <sm:hidden">
      <span class="text-gray-500 dark:text-gray-400">Signed in as</span>
      <span class="font-medium text-gray-900 dark:text-white ml-1">{{ token.name }}</span>
    </div>
    <LoginModal v-if="!isLogged" primary>
      <template v-slot:button>
        Sign in
      </template>
    </LoginModal>
    <MenuButton v-if="isLogged" @click="logout()" class="<sm:hidden">
      Logout
    </MenuButton>
    <button
      v-if="isLogged"
      type="button"
      class="hidden icon-btn default-button <sm:flex"
      aria-label="Log out"
      title="Log out"
      @click="logout()"
    >
      <LogoutIcon aria-hidden="true" />
    </button>
    <button
      type="button"
      class="icon-btn default-button text-gray-600 dark:text-gray-300"
      :aria-label="theme.isDark ? 'Switch to light theme' : 'Switch to dark theme'"
      :title="theme.isDark ? 'Switch to light theme' : 'Switch to dark theme'"
      @click="toggleTheme()"
    >
      <SunIcon v-if="theme.isDark" aria-hidden="true" />
      <MoonIcon v-else aria-hidden="true" />
    </button>
  </nav>
</template>

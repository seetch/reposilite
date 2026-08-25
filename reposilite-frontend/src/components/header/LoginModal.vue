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
import { ref } from 'vue'
import { VueFinalModal } from 'vue-final-modal'
import { createToast } from 'mosha-vue-toastify'
import { useSession } from '../../store/session'
import CloseIcon from '../icons/CloseIcon.vue'

defineProps({
  primary: {
    type: Boolean,
    default: false
  }
})

const { login } = useSession()
const showLogin = ref(false)
const name = ref('')
const secret = ref('')

const close = () => 
  (showLogin.value = false)

const signin = (name, secret) =>
  login(name, secret)
    .then(() => createToast(`Dashboard accessed as ${name}`, { position: 'bottom-right' }))
    .then(() => close())
    .catch(error => createToast(`${error.response.status}: ${error.response.data.message}`, { type: 'danger' }))
</script>

<script>
export default {
  inheritAttrs: false,
}
</script>

<template>
  <div id="login-modal">
    <VueFinalModal
      v-model="showLogin"
      v-bind="$attrs"
      class="flex justify-center items-center"
      aria-labelledby="login-dialog-title"
    >
      <div class="w-full max-w-sm surface-card shadow-surface-lg p-6 text-left">
        <div class="flex items-start justify-between">
          <div>
            <h2 id="login-dialog-title" class="font-semibold">Sign in</h2>
            <p class="text-xs text-gray-500 dark:text-gray-400">Authenticate with an access token</p>
          </div>
          <button
            type="button"
            class="icon-btn text-gray-400 hover:(bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-200) transition-colors"
            aria-label="Close login dialog"
            title="Close"
            @click="close()"
          >
            <CloseIcon aria-hidden="true" />
          </button>
        </div>
        <form class="mt-5 space-y-3" @submit.prevent="signin(name, secret)">
          <label class="block">
            <span class="text-xs text-gray-500 dark:text-gray-400">Name</span>
            <input id="login-name" v-model="name" type="text" autocomplete="username" required class="input-field mt-1 w-full px-3 py-2 text-sm"/>
          </label>
          <label class="block">
            <span class="text-xs text-gray-500 dark:text-gray-400">Secret</span>
            <input id="login-secret" v-model="secret" type="password" autocomplete="current-password" required class="input-field mt-1 w-full px-3 py-2 text-sm"/>
          </label>
          <button type="submit" class="btn-primary w-full py-2.5 text-sm">Sign in</button>
        </form>
      </div>
    </VueFinalModal>
    <button
      type="button"
      class="inline-flex items-center h-9 rounded-lg font-medium px-4 text-sm"
      :class="primary ? 'btn-primary' : 'default-button text-gray-600 dark:text-gray-300'"
      @click="showLogin = true"
    >
      <slot name="button"></slot>
    </button>
  </div>
</template>

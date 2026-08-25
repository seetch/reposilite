<script setup>
import { ref } from 'vue'
import { VueFinalModal } from 'vue-final-modal'
import { property } from '../../helpers/vue-extensions'
import CloseIcon from '../icons/CloseIcon.vue'

const props = defineProps({
  callback: property(Function, true)
})

const showFactoryReset = ref(false)

const factoryReset = () => {
  props.callback()
  showFactoryReset.value = false
}
</script>

<script>
export default {
  inheritAttrs: false
}
</script>

<template>
  <div id="adjustments-modal">
    <VueFinalModal
      v-model="showFactoryReset"
      v-bind="$attrs"
      class="flex justify-center items-center"
      aria-labelledby="factory-reset-dialog-title"
    >
      <div class="relative surface-card shadow-surface-lg max-w-110 w-[calc(100%-2rem)] py-5 px-8 text-center">
        <div>
          <h2 id="factory-reset-dialog-title" class="font-semibold tracking-tight pb-4">
            Factory reset
          </h2>
          <p>Do you really want to reset whole configuration to the default values?</p>
          <div class="flex mx-auto w-full">
            <button
              type="button"
              class="mx-auto mt-6 rounded-lg bg-red-600 px-10 py-2 text-white hover:(bg-red-700 transition-colors duration-300) <sm:px-6"
              @click="factoryReset"
            >
              Yes
            </button>
            <button
              type="button"
              class="mx-auto mt-6 rounded-lg px-10 py-2 btn-secondary <sm:px-6"
              @click="showFactoryReset = false"
            >
              No
            </button>
          </div>
        </div>
        <button
          type="button"
          class="absolute top-0 right-0 mt-4 mr-4 icon-btn text-gray-400 hover:(bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-200) transition-colors"
          aria-label="Close factory reset confirmation"
          title="Close"
          @click="showFactoryReset = false"
        >
          <CloseIcon aria-hidden="true" />
        </button>
      </div>
    </VueFinalModal>
    <button
      type="button"
      class="h-8 whitespace-nowrap rounded-lg bg-red-600 px-3 text-sm text-white hover:bg-red-700 dark:bg-red-600 dark:hover:bg-red-500 cursor-pointer"
      @click="showFactoryReset = true"
    >
      Factory reset
    </button>
  </div>
</template>

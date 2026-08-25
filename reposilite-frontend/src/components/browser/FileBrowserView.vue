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

<script setup lang="jsx">
import { computed, ref, watch } from 'vue'
import { useAdjustments } from '../../store/adjustments'
import { useSession } from '../../store/session'
import useQualifier from '../../store/qualifier'
import AdjustmentsIcon from '../icons/AdjustmentsIcon.vue'
import AdjustmentsModal from './AdjustmentsModal.vue'
import Card from '../card/SnippetsCard.vue'
import Breadcrumb from './BreadcrumbNavigation.vue'
import FileList from './FileList.vue'
import BrowserUpload from './FileUpload.vue'
import { property } from '../../helpers/vue-extensions'

const props = defineProps({
  qualifier: property(Object, true)
})

const parentPath = ref('')
const files = ref({})
const loading = ref(true)
const { details, client, hasPermissionTo } = useSession()
const { applyAdjustments } = useAdjustments()
const { getParentPath, refreshQualifier } = useQualifier()

const nonRetryableErrors = {
  401: 'You do not have permission to view this directory',
  403: 'You do not have permission to view this directory',
  404: 'Directory not found'
}

const processedFiles = computed(() => ({
  ...files.value,
  list: applyAdjustments([...files.value.list ?? []])
}))

const canUpload = computed(() => {
  return props.qualifier.path.length > 1 && hasPermissionTo(`/${props.qualifier.path}`, 'route:write')
})

watch(
  () => [props.qualifier.watchable, details.value],
  (_, __, onCleanup) => {
    if (details.value === null) {
      return
    }

    let invalidated = false
    onCleanup(() => invalidated = true)

    const qualifier = props.qualifier.path
    const isStale = () => invalidated || qualifier !== props.qualifier.path
    parentPath.value = getParentPath()
    loading.value = true

    client.value.maven.details(qualifier)
      .then(response => {
        if (isStale()) return
        files.value = {
          list: response.data.files,
          isEmpty: response.data.files.length === 0,
          error: false
        }
        loading.value = false
      })
      .catch(error => {
        if (isStale()) return
        // simulate intermediate directory if 403 & user has access to only one directory
        const status = error.response?.status
        const currentRoutes = details.value?.routes
            ?.filter(route => route.path.startsWith(`/${qualifier}`))
            ?? []

        if (status === 403 && currentRoutes.length > 0) {
          const intermediateDirectories = currentRoutes.map(currentRoute => {
            let currentSegment = currentRoute.path.substring(`/${qualifier}/`.replaceAll('//', '/').length)
            return currentSegment.includes('/') ? currentSegment.substring(0, currentSegment.indexOf('/')) : currentSegment
          })

          const uniqueIntermediateDirectories = [...new Set(intermediateDirectories)]

          files.value = {
            list: uniqueIntermediateDirectories.map(directory => ({
              name: directory,
              type: 'DIRECTORY',
              list: []
            })),
            isEmpty: false,
            error: false
          }
        } else {
          const message = nonRetryableErrors[status]

          files.value = {
            list: [],
            error: message ?? 'Could not load directory',
            retryable: message === undefined
          }
        }

        loading.value = false
      })
  },
  { immediate: true }
)

</script>

<template>
  <div class="bg-gray-100">
    <div class="dark:bg-black">
      <div class="container mx-auto min-h-320px mb-12 lg:grid lg:grid-cols-[minmax(0,1fr)_auto] lg:gap-8 lg:items-start">
        <aside class="lg:order-2 lg:pt-2" aria-label="Repository snippets">
          <Card :qualifier="qualifier" />
        </aside>
        <div class="min-w-0 lg:order-1">
          <div class="flex justify-between pt-7 px-2">
            <Breadcrumb :parentPath="parentPath" />
            <div class="flex gap-2">
              <AdjustmentsModal>
                <template v-slot:button>
                  <AdjustmentsIcon aria-hidden="true" />
                </template>
              </AdjustmentsModal>
            </div>
          </div>
          <FileList
            :qualifier="qualifier"
            :files="processedFiles"
            :loading="loading"
            :retry="refreshQualifier"
          />
          <BrowserUpload v-if="canUpload" :qualifier="qualifier" />
        </div>
      </div>
    </div>
  </div>
</template>

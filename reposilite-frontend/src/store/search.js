/*
 * Copyright (c) 2023 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ref } from 'vue'
import { createSharedComposable } from '@vueuse/core'
import { useSession } from './session'

const useSearch = createSharedComposable(() => {
  const { client } = useSession()

  const query = ref('')
  const results = ref([])
  const loading = ref(false)
  const error = ref('')

  let generation = 0

  const clear = () => {
    query.value = ''
    results.value = []
    error.value = ''
    loading.value = false
  }

  const search = (phrase, repository) => {
    const trimmedPhrase = phrase.trim()
    const currentGeneration = ++generation

    if (trimmedPhrase.length === 0) {
      results.value = []
      error.value = ''
      loading.value = false
      return
    }

    loading.value = true

    client.value.search.query(trimmedPhrase, repository)
      .then(response => {
        if (currentGeneration !== generation) return
        results.value = response.data.results
        error.value = ''
      })
      .catch(requestError => {
        if (currentGeneration !== generation) return
        results.value = []
        error.value = requestError.response?.data?.message ?? 'Search failed'
      })
      .finally(() => {
        if (currentGeneration !== generation) return
        loading.value = false
      })
  }

  return {
    query,
    results,
    loading,
    error,
    search,
    clear
  }
})

export default useSearch

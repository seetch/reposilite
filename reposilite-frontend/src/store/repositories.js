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
import { useSession } from './session'

const { client } = useSession()
const repositories = ref([])
const loaded = ref(false)

// Lightweight: reuses the same root listing the file browser uses, instead of pulling in
// the full Settings/JSON-schema machinery just to get a list of repository names.
const fetchRepositories = () =>
  client.value.maven.details('')
    .then(response => {
      repositories.value = (response.data.files || []).map(file => file.name)
      loaded.value = true
    })
    .catch(() => { repositories.value = []; loaded.value = true })

export function useRepositories() {
  return {
    repositories,
    loaded,
    fetchRepositories
  }
}

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

import { reactive, watchEffect } from 'vue'

const theme = reactive({
  mode: 'auto',
  isDark: false
})

const themeKey = 'theme-mode'

// Mirrored onto <html> (not just an inner wrapper div) so that content teleported to
// document.body - vue-final-modal dialogs, the search command palette - still resolves
// Windi's `.dark &` selectors correctly instead of rendering stuck in light mode.
watchEffect(() => {
  document.documentElement.classList.toggle('dark', theme.isDark)
})

export default function useTheme() {
  const fetchColorMode = () => {
    const storedTheme = localStorage.getItem(themeKey) ?? 'auto'
    localStorage.setItem(themeKey, storedTheme)
    theme.mode = storedTheme

    if (storedTheme === 'auto') {
      theme.isDark = window.matchMedia("(prefers-color-scheme: dark)").matches
      return
    }

    theme.isDark = storedTheme === 'dark'
  }

  const changeTheme = (mode) => {
    localStorage.setItem(themeKey, mode)
    fetchColorMode()
  }

  return {
    theme,
    fetchColorMode,
    changeTheme
  }
}

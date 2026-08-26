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

import { defineConfig } from "windicss/helpers"
import colors from "windicss/colors"

export default defineConfig({
  darkMode: "class",
  plugins: [
    require('windicss/plugin/forms')
  ],
  theme: {
    screens: {
      sm: "640px",
      md: "768px",
      lg: "1024px",
      xl: "1280px",
    },
    extend: {
      colors: {
        // Neutral graphite surfaces keep the dark theme free of blue tint.
        gray: {
          125: "#f2f4f7",
          150: "#eef1f5",
          ...colors.zinc,
        },
        accent: colors.violet,
      },
      fontFamily: {
        mono: ["JetBrains Mono", "Consolas", "Monaco", "monospace"],
      },
      boxShadow: {
        surface: "0 1px 3px rgba(0, 0, 0, 0.08)",
        "surface-lg": "0 8px 24px rgba(0, 0, 0, 0.16)",
      },
    },
  },
  shortcuts: {
    // Outlined chrome control (theme toggle, sign-in trigger, search trigger): border only,
    // hover just tints the border/text toward accent — no filled hover background.
    "default-button":
      "bg-white dark:bg-gray-900 border-1 border-gray-200 dark:border-gray-800 cursor-pointer transition-colors duration-200 hover:(border-accent-400 dark:border-accent-500 text-gray-900 dark:text-white)",
    // Text color is set explicitly (not just inherited) because dialogs built on vue-final-modal
    // teleport to document.body, which escapes the app root's `dark:text-white` ancestor entirely.
    "surface-card":
      "bg-white dark:bg-gray-900 border-1 border-gray-150 dark:border-gray-800 shadow-surface dark:shadow-none rounded-lg text-gray-800 dark:text-gray-100",
    "btn-primary":
      "bg-accent-600 text-white cursor-pointer rounded-lg font-medium hover:(bg-accent-700 transition-colors duration-200) focus-visible:(outline-none ring-2 ring-accent-400 ring-offset-2 dark:ring-offset-gray-900) disabled:(opacity-60 cursor-not-allowed)",
    "btn-secondary":
      "bg-transparent border-1 border-gray-200 dark:border-gray-800 text-gray-600 dark:text-gray-300 cursor-pointer rounded-lg transition-colors duration-200 hover:(border-accent-400 dark:border-accent-500 text-gray-900 dark:text-white)",
    "input-field":
      "bg-gray-100 dark:bg-black border-1 border-gray-200 dark:border-gray-800 rounded-lg placeholder-gray-400 focus:(outline-none border-accent-500 dark:border-accent-400) transition-colors duration-200",
    "link-accent":
      "text-accent-600 dark:text-accent-400 hover:underline",
    // Fixed square footprint for every icon-only button in the app (toolbar toggles, modal
    // close buttons, card actions) so they all read as the same control regardless of context.
    "icon-btn":
      "w-9 h-9 flex-none flex items-center justify-center rounded-lg cursor-pointer select-none",
  },
})


import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist', 'src/assets/**', 'src/components/**', 'src/pages/*.tsx', 'src/pages/delivery/**', 'src/pages/admin/AdminDashboard.tsx', 'src/pages/admin/AdminDeliveryPartners.tsx', 'src/pages/admin/AdminLayout.tsx', 'src/pages/admin/AdminOrders.tsx', 'src/pages/admin/AdminProductForm.tsx', 'src/pages/admin/AdminProducts.tsx']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
      'react-hooks/set-state-in-effect': 'off',
      'react-refresh/only-export-components': 'off',
    },
  },
])

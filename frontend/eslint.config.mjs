// @ts-check
import prettierRecommended from 'eslint-plugin-prettier/recommended'
import withNuxt from './.nuxt/eslint.config.mjs'

// Prettier owns formatting: differences are ESLint errors that lint:fix
// rewrites, and eslint-config-prettier turns off the stylistic rules that
// would disagree
export default withNuxt(prettierRecommended)

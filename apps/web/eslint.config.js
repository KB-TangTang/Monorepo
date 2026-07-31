import js from '@eslint/js';
import pluginVue from 'eslint-plugin-vue';
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting';

export default [
    {
        name: 'app/files-to-lint',
        files: ['**/*.{js,mjs,jsx,vue}'],
    },
    {
        name: 'app/files-to-ignore',
        ignores: ['**/dist/**', '**/dist-ssr/**', '**/coverage/**', '**/node_modules/**'],
    },
    js.configs.recommended,
    ...pluginVue.configs['flat/essential'],
    skipFormatting,
    {
        name: 'app/custom-rules',
        rules: {
            'vue/multi-word-component-names': 'error',
            'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
            'no-console': ['warn', { allow: ['warn', 'error'] }],
        },
    },
];

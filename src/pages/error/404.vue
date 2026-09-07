<script setup lang="ts">
import { useLangStore } from '@/stores/lang'

const lang = useLangStore()
</script>

<template>
  <div class="container" style="text-align: center; padding: 120px 0">
    <h1 style="font-family: var(--serif); font-size: 64px; color: var(--accent)">404</h1>
    <p style="margin: 16px 0 28px; color: var(--muted)">{{ lang.t('page_not_found') }}</p>
    <router-link class="btn btn-solid" to="/">{{ lang.t('back_home') }}</router-link>
  </div>
</template>

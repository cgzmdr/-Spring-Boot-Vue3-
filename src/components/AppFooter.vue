<script setup lang="ts">
import { useLangStore } from '@/stores/lang'

const lang = useLangStore()
</script>

<template>
  <footer class="footer">
    <div class="container foot">
      <div class="brand">{{ lang.t('footer_brand') }}</div>
      <div>
        <router-link to="/about">{{ lang.t('footer_about') }}</router-link>
        <span> · </span>
        <router-link to="/about">{{ lang.t('footer_source') }}</router-link>
        <span> · </span>
        <router-link to="/about">{{ lang.t('footer_copyright') }}</router-link>
        <span> · </span>
        <router-link to="/about">{{ lang.t('footer_contact') }}</router-link>
      </div>
      <div>{{ lang.t('footer_rights') }}</div>
    </div>
  </footer>
</template>

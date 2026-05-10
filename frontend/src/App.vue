<script setup>
import { ElConfigProvider } from 'element-plus/es/components/config-provider/index.mjs';
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
</script>

<template>
  <ElConfigProvider :locale="zhCn">
    <router-view />
  </ElConfigProvider>
</template>

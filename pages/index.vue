<script setup lang="ts">
import DarkModeToggle from '~/components/DarkModeToggle.vue';
import MapView from '~/components/MapView.vue';
</script>

<template>
  <div class="flex flex-col h-screen">
    <div class="flex-1">
      <MapView class="h-full"/>
    </div>
    <div>
      <Button>Click me</Button>
      <DarkModeToggle/>
    </div>
</div>
</template>

<template>
  <div class="map-container" ref="mapContainer">
    <div class="map-controls">
      <Button
        variant="outline"
        @click="onZoomInClick"
        :disabled="isZoomInDisabled"
      >
        <Icon icon="radix-icons:plus"/>
      </Button>
      <Button
        variant="outline"
        @click="onZoomOutClick"
        :disabled="isZoomOutDisabled"
      >
        <Icon icon="radix-icons:minus"/>
      </Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { ref, onMounted, onUnmounted, markRaw } from 'vue';
import { Map } from 'maplibre-gl';

const mapContainer = ref<HTMLElement|null>(null);
const map = ref<Map|null>(null);
const currentZoom = ref(14); // Initial zoom level

const isZoomInDisabled = ref(false);
const isZoomOutDisabled = ref(false);

const onZoomInClick = () => {
  map.value?.zoomIn();
};

const onZoomOutClick = () => {
  map.value!.zoomOut();
};

onMounted(() => {
  const config = useRuntimeConfig();
  const apiKey = config.public.maptilerKey;
  const initialState = { lng: 139.753, lat: 35.6844, zoom: currentZoom.value };

  map.value = markRaw(new Map({
    container: mapContainer.value!,
    style: `https://api.maptiler.com/maps/streets-v2/style.json?key=${apiKey}`,
    center: [initialState.lng, initialState.lat],
    zoom: initialState.zoom
  }));

  // Update the current zoom level when the map zoom changes
  map.value!.on('zoom', () => {
    currentZoom.value = map.value!.getZoom();
    isZoomInDisabled.value = currentZoom.value >= map.value!.getMaxZoom();
    isZoomOutDisabled.value = currentZoom.value <= 0;
  });
});

onUnmounted(() => {
  map.value?.remove();
});
</script>

<style scoped>
.map-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-controls {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 1000;
}
</style>

<template>
  <div class="map-container" ref="mapContainer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { Map, NavigationControl, GeolocateControl, Marker } from 'maplibre-gl';

const mapContainer = ref<HTMLElement | null>(null);
const map = ref<Map | null>(null);
const sourceMarker = ref<any>(null);
const destinationMarker = ref<any>(null);
const routeLayerId = 'route-layer';

// Function to add a marker to the map
const addMarker = (lngLat: any, isSource: boolean) => {
  const marker = new Marker({ color: isSource ? 'green' : 'red' })
    .setLngLat(lngLat)
    .addTo(map.value!);

  if (isSource) {
    sourceMarker.value = marker;
  } else {
    destinationMarker.value = marker;
  }
};

// Function to draw the route on the map
const drawRoute = (route: any) => {
  if (map.value!.getLayer(routeLayerId)) {
    map.value!.removeLayer(routeLayerId);
    map.value!.removeSource(routeLayerId);
  }

  map.value!.addSource(routeLayerId, {
    type: 'geojson',
    data: {
      type: 'Feature',
      geometry: {
        type: 'LineString',
        coordinates: route,
      },
    },
  });

  map.value!.addLayer({
    id: routeLayerId,
    type: 'line',
    source: routeLayerId,
    layout: {},
    paint: {
      'line-color': '#007cbf',
      'line-width': 4,
    },
  });
};

// Function to calculate route (replace with your routing service)
const calculateRoute = async (source: any, destination: any) => {
  const config = useRuntimeConfig();
  const apiKey = config.public.openRouteServiceKey;
  const url = `https://api.openrouteservice.org/v2/directions/driving-car?api_key=${apiKey}&start=${source[0]},${source[1]}&end=${destination[0]},${destination[1]}`;

  const response = await fetch(url);
  const data = await response.json();
  const route = data.features[0].geometry.coordinates;
  drawRoute(route);
};

onMounted(() => {
  const config = useRuntimeConfig();
  const apiKey = config.public.maptilerKey;

  // Initialize the map
  map.value = new Map({
    container: mapContainer.value!,
    style: `https://api.maptiler.com/maps/streets-v2/style.json?key=${apiKey}`,
    zoom: 14,
  });

  map.value.addControl(new NavigationControl());
  map.value.addControl(
    new GeolocateControl({
      positionOptions: {
        enableHighAccuracy: true,
      },
      trackUserLocation: true,
    })
  );

  let clickCount = 0;
  let source: any = null;
  let destination: any = null;

  map.value.on('click', (e: any) => {
    if (clickCount === 0) {
      source = [e.lngLat.lng, e.lngLat.lat];
      addMarker(e.lngLat, true);
      clickCount++;
    } else if (clickCount === 1) {
      destination = [e.lngLat.lng, e.lngLat.lat];
      addMarker(e.lngLat, false);
      calculateRoute(source, destination);
      clickCount = 0; // Reset for next route selection
    }
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
</style>

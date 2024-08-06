// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2024-04-03',
  devtools: { enabled: true },
  modules: ["@nuxtjs/tailwindcss", "shadcn-nuxt", "@nuxtjs/color-mode"],
  shadcn: {
    /**
     * Prefix for all the imported component
     */
    prefix: '',
    /**
     * Directory that the component lives in.
     * @default "./components/ui"
     */
    componentDir: './components/ui'
  },
  colorMode: {
    classSuffix: '',
    preference: 'dark',
  },
  css: [
    'maplibre-gl/dist/maplibre-gl.css'
  ],
  runtimeConfig: {
    public: {
      maptilerKey: process.env.MAPTILER_KEY,
      openRouteServiceKey: process.env.OPEN_ROUTE_SERVICE_KEY
    }
  }
})

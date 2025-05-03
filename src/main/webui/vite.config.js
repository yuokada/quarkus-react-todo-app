import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
// https://docs.quarkiverse.io/quarkus-quinoa/dev/web-frameworks.html#vite-config
export default defineConfig({
  base: '',
  plugins: [react()],
  // plugins: [react(), viteTsconfigPaths()],
  // server: {
  //   port: 8080,
  //   open: true
  // },
})

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
// export default defineConfig({
//   plugins: [react()],
// })
export default defineConfig({
  base: '', // depending on your application, base can also be "/"
  plugins: [react()],
  server: {
    open: false // this ensures that the browser opens upon server start
  },
})

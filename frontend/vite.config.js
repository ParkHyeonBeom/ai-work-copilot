import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api/users': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/admin': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/integrations': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/ai': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/api/briefings': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
      '/oauth2': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/login/oauth2': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
});

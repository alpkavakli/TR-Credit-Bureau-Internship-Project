import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    // Backend'in CORS ayari (SecurityConfig) sadece bu portu kabul ediyor.
    port: 5173,
    strictPort: true,
  },
});

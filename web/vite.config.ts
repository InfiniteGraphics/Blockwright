import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  base: './',
  build: {
    outDir: '../common/src/main/resources/assets/blockwright/web',
    emptyOutDir: true,
    assetsDir: 'assets',
    sourcemap: false,
    rollupOptions: {
      output: {
        entryFileNames: 'assets/blockwright-editor-[hash].js',
        chunkFileNames: 'assets/blockwright-editor-[hash].js',
        assetFileNames: 'assets/blockwright-editor-[hash][extname]'
      }
    }
  },
  server: {
    host: '127.0.0.1',
    port: 5173
  }
});

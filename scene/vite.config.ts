import { defineConfig } from 'vite';
export default defineConfig({base:'./',build:{modulePreload:false,outDir:'../app/src/main/assets/evidence-scene',emptyOutDir:true,manifest:true},server:{host:'127.0.0.1'}});

import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 9090,
    strictPort: true,
  },
  preview: {
    port: 9090,
    strictPort: true,
  },
});

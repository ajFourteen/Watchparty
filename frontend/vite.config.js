import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  // Frontend-Ebene der Teststrategie (Abschnitt 2.6): Serverdaten rein,
  // sichtbare Ausgabe raus. jsdom statt echtem Browser -- was einen echten
  // Browser braucht, gehoert auf die E2E-Ebene (Abschnitt 2.7).
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./tests/setup.js"],
    include: ["tests/**/*.test.{js,jsx}"],
    reporters: ["default", ["json", { outputFile: "../build/reports/frontend-tests.json" }]],
  },
  server: {
    proxy: {
      "/ws": {
        target: "ws://localhost:8080",
        ws: true,
      },
      "/api": {
        target: "http://localhost:8080",
      },
    },
  },
});

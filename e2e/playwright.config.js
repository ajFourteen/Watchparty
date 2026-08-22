import { defineConfig, devices } from "@playwright/test";

/**
 * E2E-Ebene der Teststrategie (docs/teststrategie.md, Abschnitt 2.7).
 *
 * Gefahren wird gegen das **gebaute Jar**, nicht gegen `vite dev`: Was hier
 * geprueft wird, ist unter anderem die Verdrahtung selbst — dass die ins Jar
 * gepackten Vite-Artefakte ausgeliefert werden, dass /join/CODE von
 * WebConfig auf die App zeigt, dass der WebSocket auf derselben Herkunft
 * liegt. Ein Dev-Server beantwortet genau diese Fragen nicht.
 *
 * `retries: 0` ist kein Versehen: Abschnitt 10 sagt, ein sporadisch
 * fehlschlagender Test ist ein Fehlschlag und keine Wiederholung. E2E ist
 * die Ebene, auf der diese Regel zuerst wehtut — deshalb steht sie hier
 * ausdruecklich und nicht als Standardwert.
 *
 * Video und Trace laufen immer mit, nicht nur bei Fehlschlaegen: Die
 * Durchlaeufe dienen ausdruecklich auch der Vorfuehrung (`npm run vorfuehren`
 * spielt sie sichtbar und verlangsamt ab).
 */
const PORT = process.env.WATCHPARTY_PORT ?? "8099";

export default defineConfig({
  testDir: "./tests",
  // Postgres aus Testcontainers plus das gebaute Jar davor, einmal je Lauf
  // (tests/umgebung.js). Beides hier statt ueber Playwrights `webServer`:
  // Die Anwendung braucht die JDBC-URL, die erst der Container vergibt.
  globalSetup: "./tests/global-setup.js",
  globalTeardown: "./tests/global-teardown.js",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  // Ein vollstaendiger Nutzerweg durch einen echten Browser -- zweimal
  // anmelden, tippen, Liga gruenden, beitreten -- braucht rund 40 Sekunden.
  // Das Budget ist danach bemessen und nicht nach einem runden Wert; zu eng
  // gesetzt macht es aus langsamer Arbeit einen Fehlschlag.
  timeout: 120_000,
  expect: { timeout: 10_000 },
  reporter: [["html", { outputFolder: "../build/reports/e2e", open: "never" }], ["list"]],
  use: {
    baseURL: `http://localhost:${PORT}`,
    video: "on",
    trace: "on",
    screenshot: "only-on-failure",
    ...devices["Desktop Chrome"],
  },
});

/**
 * Der Aufbau der E2E-Umgebung: ein Postgres aus Testcontainers und das
 * gebaute Jar davor.
 *
 * **Datenbanken kommen in diesem Projekt immer aus Testcontainers**
 * (docs/teststrategie.md, Abschnitt 10) — auch hier, nicht nur im Backend.
 * Der erste Entwurf dieser Ebene startete den Container selbst per
 * `docker run` und liess Playwright einen laufenden Server weiterverwenden;
 * dadurch trug jeder Lauf den Zustand des vorherigen mit sich und die
 * Szenarien wurden voneinander abhaengig. Testcontainers macht die
 * Isolierung zur Eigenschaft des Aufbaus: Der Container entsteht mit dem
 * Lauf und verschwindet mit ihm, so wie {@code PostgresAdapterSupport} es
 * den Backend-Ebenen gibt.
 *
 * Server und Container werden hier gestartet und nicht ueber Playwrights
 * `webServer`-Eintrag: Die Anwendung braucht die JDBC-URL, die erst der
 * Container vergibt, und die Reihenfolge zwischen `webServer` und
 * `globalSetup` ist nichts, worauf man sich verlassen sollte.
 *
 * Gefahren wird gegen das **gebaute Jar**, nicht gegen `vite dev`: Geprueft
 * wird unter anderem die Verdrahtung des Auslieferungsstandes — die ins Jar
 * gepackte React-App, die Weiterleitung von /join/CODE, der WebSocket auf
 * derselben Herkunft. Ein Dev-Server beantwortet diese Fragen nicht.
 */
import { spawn } from "node:child_process";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import { PostgreSqlContainer } from "@testcontainers/postgresql";

const HIER = dirname(fileURLToPath(import.meta.url));
const WURZEL = resolve(HIER, "..", "..");
const JAR = join(WURZEL, "build", "libs", "watchparty-0.1.0.jar");

let container;
let server;

export async function starteUmgebung(port) {
  container = await new PostgreSqlContainer("postgres:16-alpine").start();

  server = spawn(
    "java",
    [
      "-jar", JAR,
      `--server.port=${port}`,
      `--watchparty.snapshot.path=${mkdtempSync(join(tmpdir(), "watchparty-e2e-"))}`,
      `--watchparty.league.db.url=jdbc:postgresql://${container.getHost()}:${container.getPort()}/${container.getDatabase()}`,
      `--watchparty.league.db.username=${container.getUsername()}`,
      `--watchparty.league.db.password=${container.getPassword()}`,
      // Das Wettfenster laenger als die Vorgabe: Ein ganzer Rundenablauf
      // durch einen echten Browser passt nicht zuverlaessig in 15 Sekunden
      // echte Uhrzeit. Ohne das war das Szenario sporadisch rot -- und ein
      // sporadisch roter Test ist nach Abschnitt 10 ein Fehlschlag.
      "--watchparty.betting-window-seconds=300",
      "--watchparty.league.schedule.season-year=2026",
      "--watchparty.league.schedule.relay-token=e2e-nicht-benutzt",
      "--watchparty.league.admin.email=admin@example.org",
      "--watchparty.league.session.cookie-secure=false",
    ],
    { stdio: ["ignore", "pipe", "pipe"] },
  );

  server.stdout.on("data", (d) => process.stdout.write(`[server] ${d}`));
  server.stderr.on("data", (d) => process.stderr.write(`[server] ${d}`));

  // Die Zugangsdaten an die Testprozesse weiterreichen: tests/db.js braucht
  // sie, um den Anmeldelink zu lesen. Playwright startet die Worker nach dem
  // globalen Aufbau, sie erben diese Werte also.
  process.env.WATCHPARTY_DB_HOST = container.getHost();
  process.env.WATCHPARTY_DB_PORT = String(container.getPort());
  process.env.WATCHPARTY_DB_USER = container.getUsername();
  process.env.WATCHPARTY_DB_PASSWORD = container.getPassword();
  process.env.WATCHPARTY_DB_NAME = container.getDatabase();

  await warteAufAnwendung(port);
  return container;
}

export async function stoppeUmgebung() {
  server?.kill("SIGTERM");
  await container?.stop();
}

/** Auf die Bedingung warten statt auf eine feste Zeit (Abschnitt 10). */
async function warteAufAnwendung(port) {
  const bis = Date.now() + 180_000;
  while (Date.now() < bis) {
    if (server?.exitCode !== null && server?.exitCode !== undefined) {
      throw new Error(`Die Anwendung ist beim Hochfahren beendet worden (Code ${server.exitCode}).`);
    }
    try {
      const antwort = await fetch(`http://localhost:${port}/`);
      if (antwort.ok) return;
    } catch {
      // noch nicht da
    }
    await new Promise((fertig) => setTimeout(fertig, 300));
  }
  throw new Error("Die Anwendung war nicht innerhalb von 180 s erreichbar.");
}

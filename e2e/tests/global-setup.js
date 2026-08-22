import { starteUmgebung } from "./umgebung.js";

/** Ein frisches Postgres und die Anwendung davor, einmal je Lauf. */
export default async function globalSetup() {
  await starteUmgebung(process.env.WATCHPARTY_PORT ?? "8099");
}

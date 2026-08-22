import { stoppeUmgebung } from "./umgebung.js";

export default async function globalTeardown() {
  await stoppeUmgebung();
}

/**
 * Der Anforderungs-Tag der Frontend-Ebene — das Gegenstueck zu
 * `@Anforderung` im Backend (docs/teststrategie.md, Abschnitt 5.1).
 *
 * Er tut zweierlei: Er stellt die IDs dem Leser des Testlaufs voran, und er
 * macht sie im Quelltext maschinell auffindbar. Der Gradle-Task
 * `abdeckungFrontend` liest genau diese Aufrufe und gleicht sie mit den
 * `frontend`-markierten Regeln aus Anhang A ab — deshalb muessen die IDs
 * hier als Zeichenketten stehen und duerfen nicht erst zur Laufzeit
 * entstehen.
 */
import { it } from "vitest";

export function anforderung(ids, name, fn) {
  const liste = Array.isArray(ids) ? ids : [ids];
  return it(`${liste.join(", ")}: ${name}`, fn);
}

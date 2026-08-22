/**
 * Derselbe Anforderungs-Tag wie auf der Frontend-Ebene
 * (frontend/tests/anforderung.js) — `abdeckungFrontend` liest beide Orte.
 * Bewusst kopiert statt geteilt: e2e/ ist ein eigenes npm-Projekt mit
 * eigenen Abhaengigkeiten, ein Import ueber die Projektgrenze waere eine
 * Kopplung fuer drei Zeilen.
 */
import { test } from "@playwright/test";

export function anforderung(ids, name, fn) {
  const liste = Array.isArray(ids) ? ids : [ids];
  return test(`${liste.join(", ")}: ${name}`, fn);
}

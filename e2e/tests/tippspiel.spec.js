/**
 * E2E-Ebene, kritischer Pfad des Tippspiels (docs/teststrategie.md,
 * Abschnitt 2.7).
 *
 * Dieser Weg war bis 2026-08-21 der am schlechtesten abgesicherte des ganzen
 * Projekts: CLAUDE.md fuehrte ihn als "nur manuell per curl durchgespielt,
 * nicht in einem echten Browser". Er spannt ueber alles, was einzeln laengst
 * geprueft ist — Anmeldelink, Sitzungscookie, Tipp, Liga, Rangliste — und
 * genau die Uebergaenge dazwischen hat vorher niemand als Ganzes gesehen.
 *
 * Auch hier gilt Abschnitt 2.7: keine neue fachliche Abdeckung. Ob die
 * Wertung stimmt, hat Scoring entschieden; ob ein fremder Tipp verborgen
 * bleibt, PredictionView und der Leck-Test am JSON. Hier zaehlt, dass die
 * Kette haelt.
 */
import { expect, test } from "@playwright/test";
import { anmeldelinkFuer, spielAnlegen } from "./db.js";

const SAISON = 2026;
// Der Spieltagsschirm startet immer bei Woche 1 (MatchdayScreen), also wird
// hier auch dort gearbeitet -- kein Blaettern, kein Zustand, der davon
// abhaengt, welcher Spieltag gerade laeuft.
const WOCHE = 1;

/** Anmelden wie ein Mensch: Adresse eingeben, Link oeffnen, drin sein. */
async function anmelden(page, email, name) {
  await page.goto("/league");
  await page.getByPlaceholder("E-Mail-Adresse").fill(email);
  await page.getByPlaceholder("Anzeigename").fill(name);
  await page.getByRole("button", { name: /Anmeldelink/ }).click();

  // Die Bestaetigung ist bewusst dieselbe, egal ob es das Konto gibt
  // (Kriterium 3) -- der Test darf daraus also nichts ableiten.
  await expect(page.locator(".league-card")).toContainText("Fast geschafft");

  const token = await anmeldelinkFuer(email);
  await page.goto(`/league/login/${token}`);

  // Der Link wird nicht schon vom Oeffnen verbraucht, sondern erst durch
  // diesen Klick -- damit die Vorschau eines Mail-Programms ihn nicht
  // entwertet. Ein Zwischenschritt, den man nur im echten Browser sieht.
  await page.getByRole("button", { name: "Jetzt anmelden" }).click();
  await expect(page.getByRole("button", { name: "Anleitung" })).toBeVisible();
  await anleitungSchliessen(page);
}

async function anleitungSchliessen(page) {
  const overlay = page.locator(".overlay");
  if (await overlay.isVisible()) {
    await page.getByRole("button", { name: "Schließen" }).click();
    await expect(overlay).toHaveCount(0);
  }
}

test.describe("Der kritische Pfad des Tippspiels", () => {
  test("anmelden, tippen, Liga gruenden, beitreten und die Rangliste lesen", async ({ browser }) => {
    // Sprechende, feste Namen: Die Unabhaengigkeit vom vorherigen Lauf
    // leistet das Leeren der Tabellen (tests/db-leeren.js), nicht ein
    // Zeitstempel im Namen. Das haelt zugleich die Aufzeichnung lesbar --
    // dieser Durchlauf dient auch der Vorfuehrung.
    const heimteam = "Green Bay Packers";
    const gastteam = "Chicago Bears";
    const ligaName = "E2E-Liga";

    await spielAnlegen({
      id: "e2e-spiel-1",
      saison: SAISON,
      woche: WOCHE,
      heim: heimteam,
      gast: gastteam,
      anstoss: new Date(Date.now() + 3600_000).toISOString(),
      status: "SCHEDULED",
    });

    const annaGeraet = await browser.newContext();
    const benGeraet = await browser.newContext();
    const anna = await annaGeraet.newPage();
    const ben = await benGeraet.newPage();

    await anmelden(anna, "anna@example.org", "Anna");
    await anmelden(ben, "ben@example.org", "Ben");

    // --- Tippen ------------------------------------------------------
    await anna.getByRole("button", { name: "Spieltag" }).click();

    // Gezielt die Karte dieses Laufs, nicht "die erste" -- in Woche 1 koennen
    // Spiele frueherer Laeufe stehen.
    const spiel = anna.locator(".game-card").filter({ hasText: heimteam });
    await expect(spiel).toContainText(gastteam);
    // Reihenfolge der Felder wie im Formular: erst der Gast, dann der
    // Gastgeber (PredictionForm). Ueber die Beschriftung angesprochen statt
    // ueber die Position, damit ein Umsortieren auffaellt statt still den
    // Tipp zu vertauschen.
    await spiel.getByRole("textbox", { name: gastteam }).fill("17");
    await spiel.getByRole("textbox", { name: heimteam }).fill("24");
    await spiel.getByRole("button", { name: "Tipp abgeben" }).click();

    // Vor dem Anstoss steht der eigene Tipp in den Feldern, und der Knopf
    // heisst jetzt "Tipp ändern" -- der Beleg, dass der Server ihn
    // angenommen hat und die Ansicht ihn zurueckbekommt (Kriterium 16).
    await expect(spiel.getByRole("button", { name: "Tipp ändern" })).toBeVisible();
    await expect(spiel.getByRole("textbox", { name: heimteam })).toHaveValue("24");
    await expect(spiel.getByRole("textbox", { name: gastteam })).toHaveValue("17");

    // --- Liga gruenden und den Code weitergeben -----------------------
    await anna.getByRole("button", { name: "Ligen" }).click();
    await anna.getByPlaceholder("Name der Liga").fill(ligaName);
    await anna.locator(".league-form").getByRole("button", { name: "Anlegen" }).click();

    await anna.getByRole("button", { name: new RegExp(ligaName) }).click();
    const code = (await anna.locator(".room-code").first().textContent()).trim();
    expect(code).toMatch(/^[0-9A-Z]{6}$/);

    // --- Ben tritt mit dem Code bei -----------------------------------
    await ben.getByRole("button", { name: "Ligen" }).click();
    await ben.getByPlaceholder("Beitrittscode").fill(code);
    await ben.getByRole("button", { name: "Beitreten" }).click();
    await expect(ben.getByRole("button", { name: new RegExp(ligaName) })).toBeVisible();

    // --- Die Rangliste zeigt beide -------------------------------------
    await ben.getByRole("button", { name: new RegExp(ligaName) }).click();
    await expect(ben.locator(".league-card")).toContainText("Anna");
    await expect(ben.locator(".league-card")).toContainText("Ben");

    await annaGeraet.close();
    await benGeraet.close();
  });
});

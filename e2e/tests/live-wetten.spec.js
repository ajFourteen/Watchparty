/**
 * E2E-Ebene, kritischer Pfad der Live-Wetten (docs/teststrategie.md,
 * Abschnitt 2.7).
 *
 * Diese Ebene fuegt **keine neue fachliche Abdeckung** hinzu — dieselbe
 * Zusage wie die API-Ebene in Abschnitt 2.4. Sie beantwortet die drei
 * Fragen, die keine andere Ebene beantworten kann:
 *
 *  1. Traegt das gebaute Jar? Die ins Jar gepackte React-App, die
 *     Weiterleitung von /join/CODE, der WebSocket auf derselben Herkunft.
 *  2. Halten mehrere Geraete gleichzeitig zusammen? Zwei Browser-Kontexte
 *     sind Host und Mitspieler — die einzige Ebene, auf der Invariante 4
 *     dort geprueft wird, wo sie fachlich gilt: im zweiten Browser.
 *  3. Ueberlebt der Beitritt einen Neuladen des Tabs?
 *
 * Der Durchlauf ist zugleich die Vorfuehrung: ein vollstaendiger
 * Rundenablauf von zwei Seiten, mit Video.
 */
import { expect, test } from "@playwright/test";
import { anforderung } from "./anforderung.js";

/**
 * Beim ersten Beitritt auf einem Geraet geht die Kurzanleitung von selbst
 * auf und legt sich als Overlay ueber die Seite. Ein echter Mensch schliesst
 * sie -- und muss das auch, denn sie faengt jeden Klick ab. Genau diese Art
 * Fund ist der Grund fuer die E2E-Ebene: In jsdom faellt es nicht auf, weil
 * dort nichts etwas ueberdeckt.
 */
async function anleitungSchliessen(page) {
  const overlay = page.locator(".overlay");
  if (await overlay.isVisible()) {
    await page.getByRole("button", { name: "Schließen" }).click();
    await expect(overlay).toHaveCount(0);
  }
}

/** Beitreten und den vergebenen Raumcode zurueckgeben. */
async function raumErstellen(page, name) {
  await page.goto("/");
  await page.getByPlaceholder("Dein Name").fill(name);
  await page.getByRole("button", { name: "Raum erstellen" }).click();
  const code = page.locator(".room-code");
  await expect(code).toBeVisible();
  await anleitungSchliessen(page);
  return (await code.textContent()).trim();
}

async function mitspielen(page, name, code) {
  await page.goto("/");
  await page.getByPlaceholder("Dein Name").fill(name);
  await page.getByPlaceholder("Code (optional)").fill(code);
  await page.getByRole("button", { name: "Mitspielen" }).click();
  await expect(page.locator(".room-code")).toHaveText(code);
  await anleitungSchliessen(page);
}

async function tippAbgeben(page, ausgang, einsatz) {
  await page.locator(".board").getByRole("button", { name: ausgang }).click();
  const feld = page.locator(".stake-field");
  await feld.fill(String(einsatz));
  await page.getByRole("button", { name: "Tipp abgeben" }).click();
  // Nicht auf ".locked" warten: Ist es der letzte fehlende Tipp, schliesst
  // der Server das Fenster sofort (5-g) und die Ansicht springt an ".locked"
  // vorbei direkt in die Aufdeckung. Dass das Formular weg ist, gilt in
  // beiden Faellen.
  await expect(page.locator(".stake-field")).toHaveCount(0);
}

test.describe("Ein vollstaendiger Rundenablauf mit zwei Geraeten", () => {
  anforderung(
    ["1-f", "10-c"],
    "vom Beitritt ueber den verdeckten Tipp bis zum verrechneten Ergebnis",
    async ({ browser }) => {
      // Zwei echte Geraete: getrennte Kontexte, getrennter localStorage,
      // getrennte Verbindung. Ein einzelner Browser mit zwei Tabs waere das
      // nicht -- er teilte sich den Token-Speicher.
      const hostGeraet = await browser.newContext();
      const gastGeraet = await browser.newContext();
      const host = await hostGeraet.newPage();
      const gast = await gastGeraet.newPage();

      // 1-f: Teilnahme ohne Installation -- eine URL im Browser genuegt.
      const code = await raumErstellen(host, "Anna");
      await mitspielen(gast, "Ben", code);

      await expect(host.locator(".roster li")).toHaveCount(2);

      // Der Host oeffnet die erste Wette aus dem Katalog.
      await host.locator(".host .chooser button").first().click();
      await expect(host.locator(".counter")).toBeVisible();
      await expect(gast.locator(".counter")).toBeVisible();

      const ausgang = await gast.locator(".board .options button").first().textContent();
      await tippAbgeben(gast, ausgang.trim(), 200);

      // Invariante 4 im zweiten Browser: Der Host sieht den Zaehler steigen,
      // aber nirgends, *was* Ben getippt hat. Geprueft am sichtbaren Text der
      // ganzen Seite, nicht an einem einzelnen Element -- ein Leck koennte
      // ueberall auftauchen.
      await expect(host.locator(".counter")).toHaveText("1 von 2 haben getippt");
      await expect(host.locator(".shell")).not.toContainText("Ben " + ausgang.trim());
      await expect(host.locator(".reveal")).toHaveCount(0);

      await tippAbgeben(host, ausgang.trim(), 300);

      // Alle haben getippt -> das Fenster schliesst von selbst (5-g/5-h),
      // und erst jetzt werden die Tipps aufgedeckt (Anforderung 9).
      await expect(host.locator(".all-picked")).toBeVisible();
      await expect(gast.locator(".reveal li")).toHaveCount(2);

      // Auflösen: derselbe Ausgang, den beide getippt haben -- ein Push.
      await host.locator(".board .options button").filter({ hasText: ausgang.trim() }).click();
      await expect(host.locator(".result")).toBeVisible();
      await expect(gast.locator(".result")).toBeVisible();
      await expect(gast.locator(".result")).toContainText("Pool: 500 Punkte");

      // Nullsumme am Bildschirm: beide bekommen ihren Einsatz zurueck.
      await expect(gast.locator(".result .reveal")).toContainText("+0");

      await hostGeraet.close();
      await gastGeraet.close();
    }
  );

  anforderung("1-l", "ein /join/CODE-Link fuellt den Code im echten Browser vor", async ({ browser }) => {
    const hostGeraet = await browser.newContext();
    const gastGeraet = await browser.newContext();
    const host = await hostGeraet.newPage();
    const gast = await gastGeraet.newPage();

    const code = await raumErstellen(host, "Anna");

    // Der Weg, den ein weitergereichter Link nimmt: WebConfig leitet
    // /join/CODE auf index.html, die App liest ihn aus dem Pfad.
    await gast.goto(`/join/${code}`);
    await expect(gast.getByPlaceholder("Code (optional)")).toHaveValue(code);

    await gast.getByPlaceholder("Dein Name").fill("Ben");
    await gast.getByRole("button", { name: "Mitspielen" }).click();
    await expect(gast.locator(".room-code")).toHaveText(code);

    await hostGeraet.close();
    await gastGeraet.close();
  });

  // Bewusst ohne Anforderungs-Tag: Reconnect ist in Anhang A backend-markiert
  // und auf der Port-Ebene abgedeckt (ReconnectTest). Was hier dazukommt, ist
  // allein der Browser-Anteil -- der Token im localStorage und das Neuladen
  // des Tabs. Ein Tag mit der backend-ID wuerde dieselbe Regel ein zweites
  // Mal als Abdeckung zaehlen und Abschnitt 7.4 unterlaufen.
  test("ein Neuladen des Tabs bringt denselben Spieler zurueck", async ({ browser }) => {
    const geraet = await browser.newContext();
    const seite = await geraet.newPage();

    const code = await raumErstellen(seite, "Anna");
    await expect(seite.locator(".bug-value")).toHaveText("1000");

    await seite.reload();
    await expect(seite.locator(".room-code")).toBeVisible();

    // Kein neues Beitrittsformular, kein zweiter Spieler: derselbe Token
    // aus dem localStorage, derselbe Platz am Tisch (ADR-014).
    await expect(seite.locator(".room-code")).toHaveText(code);
    await expect(seite.locator(".roster li")).toHaveCount(1);

    await geraet.close();
  });
});

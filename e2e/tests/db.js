/**
 * Direkter Blick in die Datenbank — ausschliesslich, um an den Anmeldelink
 * zu kommen.
 *
 * Der Link geht im Betrieb per Mail hinaus; ohne SMTP-Zugangsdaten schreibt
 * ihn der LoggingMailSender nur ins Log. Ein E2E-Test kann kein Postfach
 * oeffnen, also holt er den Token dort, wo er entsteht. Das ist der einzige
 * Punkt, an dem diese Ebene an der Oberflaeche vorbeigreift — und er ist
 * bewusst so eng: gelesen wird ein Token, sonst nichts. Alles Uebrige
 * geschieht durch Klicken.
 */
import pg from "pg";

// Der globale Aufbau (tests/umgebung.js) legt diese Werte ab, sobald der
// Testcontainer steht -- Host und Port vergibt der Container, nicht wir.
const VERBINDUNG = {
  host: process.env.WATCHPARTY_DB_HOST,
  port: Number(process.env.WATCHPARTY_DB_PORT),
  user: process.env.WATCHPARTY_DB_USER,
  password: process.env.WATCHPARTY_DB_PASSWORD,
  database: process.env.WATCHPARTY_DB_NAME,
};

export async function anmeldelinkFuer(email) {
  const client = new pg.Client(VERBINDUNG);
  await client.connect();
  try {
    const { rows } = await client.query(
      "SELECT token FROM login_link WHERE email = $1 ORDER BY created_at DESC LIMIT 1",
      [email],
    );
    if (rows.length === 0) {
      throw new Error(`Kein Anmeldelink fuer ${email} in der Datenbank.`);
    }
    return rows[0].token;
  } finally {
    await client.end();
  }
}

/** Ein gewertetes Spiel anlegen, das es im Feed nicht gibt. */
export async function spielAnlegen({ id, saison, woche, heim, gast, anstoss, status, heimPunkte, gastPunkte }) {
  const client = new pg.Client(VERBINDUNG);
  await client.connect();
  try {
    await client.query(
      `INSERT INTO game (id, season_year, week, home_team_id, home_team_name, away_team_id,
                         away_team_name, kickoff, status, home_score, away_score, manual_override)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, false)
       ON CONFLICT (id) DO UPDATE SET kickoff = EXCLUDED.kickoff, status = EXCLUDED.status,
                                      home_score = EXCLUDED.home_score, away_score = EXCLUDED.away_score`,
      [id, saison, woche, heim.slice(0, 4), heim, gast.slice(0, 4), gast, anstoss, status,
       heimPunkte ?? null, gastPunkte ?? null],
    );
  } finally {
    await client.end();
  }
}

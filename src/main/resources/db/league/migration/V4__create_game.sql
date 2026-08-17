-- Spiele eines Spieltags (Feature 005, Stufe 4, ADR-037). Mannschaften
-- denormalisiert mitgefuehrt statt in einer eigenen Tabelle (Team ist ein
-- Value Object, keine eigene Identitaet im Modell) -- der Feed liefert sie
-- ohnehin zu jedem Spiel neu.
CREATE TABLE game (
    id TEXT PRIMARY KEY,
    season_year INT NOT NULL,
    week INT NOT NULL,
    home_team_id TEXT NOT NULL,
    home_team_name TEXT NOT NULL,
    away_team_id TEXT NOT NULL,
    away_team_name TEXT NOT NULL,
    kickoff TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL,
    home_score INT,
    away_score INT,
    manual_override BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_game_matchday ON game (season_year, week);

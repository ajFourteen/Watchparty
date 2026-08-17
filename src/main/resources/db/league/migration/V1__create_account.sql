-- Konto eines Tippers (Feature 005, Stufe 2/3). Die E-Mail-Adresse ist die
-- Identitaet -- kein separates AccountId, ein Feld und ein Index weniger
-- (so wenig personenbezogene Daten wie moeglich). Bereits kleingeschrieben
-- gespeichert -- die Normalisierung traegt EmailAddress
-- (domain/model/league), nicht diese Migration.
CREATE TABLE account (
    email TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

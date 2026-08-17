-- Konto eines Tippers (Feature 005, Stufe 2/3). E-Mail eindeutig und bereits
-- kleingeschrieben gespeichert -- die Normalisierung traegt EmailAddress
-- (domain/model/league), nicht diese Migration.
CREATE TABLE account (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

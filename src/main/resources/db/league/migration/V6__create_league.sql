-- Ligen und ihre Mitgliedschaften (Feature 005, Stufe 6). Membership lebt
-- innerhalb des League-Aggregats, ist aber eine eigene Tabelle -- eine
-- Liga hat beliebig viele Mitglieder.
CREATE TABLE league (
    id UUID PRIMARY KEY,
    season_year INT NOT NULL,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    manager_email TEXT NOT NULL REFERENCES account(email)
);

CREATE TABLE league_membership (
    league_id UUID NOT NULL REFERENCES league(id),
    account_email TEXT NOT NULL REFERENCES account(email),
    joined_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (league_id, account_email)
);

CREATE INDEX idx_league_membership_account ON league_membership (account_email);

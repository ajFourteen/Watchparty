-- Angeforderte Anmeldelinks (Feature 005, Stufe 3, ADR-036). Keine
-- Fremdschluessel-Bindung an account: Ein Link kann zu einer Adresse
-- ausgestellt werden, zu der es noch gar kein Konto gibt (Kriterium 1).
CREATE TABLE login_link (
    token TEXT PRIMARY KEY,
    email TEXT NOT NULL,
    display_name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

-- Angemeldete Sitzungen (Feature 005, Stufe 3, Kriterium 5). Anders als
-- login_link an account gebunden: Eine Sitzung entsteht erst, nachdem das
-- Konto beim Einloesen angelegt oder gefunden wurde.
CREATE TABLE account_session (
    token TEXT PRIMARY KEY,
    account_email TEXT NOT NULL REFERENCES account(email),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

-- Ergebnistipps (Feature 005, Stufe 5). Identitaet ist das Paar aus Konto
-- und Spiel (PredictionId) -- ein neuer Tipp ersetzt den bestehenden ueber
-- denselben Primaerschluessel (Kriterium 16), keine Historie noetig.
CREATE TABLE prediction (
    account_email TEXT NOT NULL REFERENCES account(email),
    game_id TEXT NOT NULL REFERENCES game(id),
    home_score INT NOT NULL,
    away_score INT NOT NULL,
    PRIMARY KEY (account_email, game_id)
);

CREATE INDEX idx_prediction_game ON prediction (game_id);

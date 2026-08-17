package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Die Identität eines {@link Prediction}: das Paar aus Konto und Spiel
 * (Feature-Dokument, "ein Tipp gehört dem Tipper und dem Spiel"). Genau
 * dieses Paar sorgt strukturell dafür, dass ein Tipper zu einem Spiel nur
 * einen einzigen Ergebnistipp haben kann — ein zweiter mit demselben Paar
 * ersetzt den ersten, statt einen weiteren anzulegen.
 */
@ValueObject
public record PredictionId(EmailAddress accountEmail, GameId gameId) {

    public static PredictionId of(EmailAddress accountEmail, GameId gameId) {
        return new PredictionId(accountEmail, gameId);
    }
}

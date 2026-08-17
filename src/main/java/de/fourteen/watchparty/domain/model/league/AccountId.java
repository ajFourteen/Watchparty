package de.fourteen.watchparty.domain.model.league;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.UUID;

/**
 * Die Identitaet eines {@link Account}. Value Object.
 *
 * Anders als {@link de.fourteen.watchparty.domain.model.RoomCode} muss ein
 * Konto nie vorgelesen oder von Hand eingetippt werden — eine {@link UUID}
 * reicht, ein eigener lesbarer Code waere hier Aufwand ohne Gegenwert.
 */
@ValueObject
public record AccountId(UUID value) {

    public AccountId {
        if (value == null) {
            throw new IllegalArgumentException("Eine Konto-ID ist nie null");
        }
    }

    public static AccountId newId() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId of(UUID value) {
        return new AccountId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

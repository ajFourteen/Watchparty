package de.fourteen.watchparty.adapter.in.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * Uebersetzt die wenigen Ausnahmen der Ligakommandos in HTTP-Status, an
 * einer Stelle statt in jedem Controller einzeln.
 */
@RestControllerAdvice(basePackages = "de.fourteen.watchparty.adapter.in.http")
class LeagueExceptionHandler {

    @ExceptionHandler(NotAuthenticatedException.class)
    ResponseEntity<Void> notAuthenticated() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /** Kriterium 13.3-h: eine gueltige Sitzung, aber kein Admin-Konto. */
    @ExceptionHandler(NotAuthorizedException.class)
    ResponseEntity<Void> notAuthorized() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Void> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /** Kriterium 16: der Anstoss ist bereits erfolgt. */
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Void> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    /** Ungueltige Eingabe, z. B. ein zu langer Anzeigename oder ein negatives Ergebnis. */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Void> badRequest() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}

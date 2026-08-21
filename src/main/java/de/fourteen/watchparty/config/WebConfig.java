package de.fourteen.watchparty.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Leitet Pfade, die nur die Single-Page-App selbst versteht, auf
 * {@code index.html} weiter. Spring Boots eingebaute "Welcome Page"
 * liefert sie sonst nur fuer {@code /} aus (ADR-033) -- ohne diese Regeln
 * liefen geteilte Links ins Leere.
 *
 * {@code /join/CODE} (Anforderung 1-l): das Frontend liest den Code selbst
 * aus dem Pfad und fuellt damit das Beitrittsformular vor, der Server weiss
 * von Watchparty-Codes an dieser Stelle nichts.
 *
 * {@code /league} und {@code /league/login/TOKEN} (Kriterium 1, ADR-039):
 * derselbe Grund fuer den zweiten Spielmodus -- der Anmeldelink aus der
 * Mail zeigt auf einen Pfad, den nur das React-Routing im Frontend
 * versteht. Ungated, anders als die Ligakommandos selbst: Ohne Datenbank
 * bleibt die Liga zwar funktionslos, aber die Seite soll trotzdem laden,
 * nicht mit einem rohen 404 enden (Kriterium 37 sinngemaess).
 * {@code /league/report-mail/unsubscribe/TOKEN} (13.9-p, ADR-041): derselbe
 * Grund fuer den Abmeldelink aus der Report-Mail.
 *
 * {@code /impressum} und {@code /datenschutz} (Stufe 8, Feature 005):
 * gelten fuer beide Spielmodi gleichermassen, deshalb ausserhalb von
 * {@code /league} und ohne jede Bedingung -- die Rechtstexte muessen auch
 * ohne Datenbank erreichbar sein.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/join/{code}").setViewName("forward:/index.html");
        registry.addViewController("/league").setViewName("forward:/index.html");
        registry.addViewController("/league/login/{token}").setViewName("forward:/index.html");
        registry.addViewController("/league/report-mail/unsubscribe/{token}").setViewName("forward:/index.html");
        registry.addViewController("/impressum").setViewName("forward:/index.html");
        registry.addViewController("/datenschutz").setViewName("forward:/index.html");
    }
}

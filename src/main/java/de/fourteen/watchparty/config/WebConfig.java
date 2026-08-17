package de.fourteen.watchparty.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Leitet den Beitrittslink {@code /join/CODE} (Anforderung 1-l) auf die
 * Single-Page-App weiter. Spring Boots eingebaute "Welcome Page" liefert
 * {@code index.html} nur fuer {@code /} aus (ADR-033) -- ohne diese Regel
 * liefe ein geteilter Link ins Leere. Das Frontend liest den Code selbst
 * aus dem Pfad und fuellt damit das Beitrittsformular vor; der Server weiss
 * von Watchparty-Codes an dieser Stelle nichts.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/join/{code}").setViewName("forward:/index.html");
    }
}

-- Mailversand des Spieltags-Reports (13.9-n/p, Feature 010, ADR-041):
-- Opt-in am Konto und der stabile Token fuer den Ein-Klick-Abmeldelink.
-- Bestehende Konten bekommen den Token einmalig beim Anlegen der Spalte,
-- damit report_mail_token nie NULL ist (Account traegt ihn als
-- Pflichtfeld) -- ohne Opt-in bleiben sie davon unberuehrt. md5() aus dem
-- Kern statt gen_random_bytes() aus der pgcrypto-Extension, die auf einem
-- unmanaged Fly Postgres (ADR-035) nicht ohne Weiteres verfuegbar ist --
-- fuer einen einmaligen Bestandsschutz reicht das, neue Konten bekommen
-- ihren Token weiterhin ueber ReportMailToken.generate() (SecureRandom).
ALTER TABLE account ADD COLUMN report_mail_opt_in BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE account ADD COLUMN report_mail_token TEXT;
UPDATE account SET report_mail_token = md5(random()::text || clock_timestamp()::text || email)
    WHERE report_mail_token IS NULL;
ALTER TABLE account ALTER COLUMN report_mail_token SET NOT NULL;
ALTER TABLE account ADD CONSTRAINT account_report_mail_token_unique UNIQUE (report_mail_token);

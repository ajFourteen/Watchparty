# [3.9.0](https://github.com/ajFourteen/Watchparty/compare/v3.8.1...v3.9.0) (2026-08-18)


### Bug Fixes

* fehlende Änderungen zum Feed-Relay nachtragen ([7035741](https://github.com/ajFourteen/Watchparty/commit/7035741fecf57db54aaf9ba26345d11fd283aebb))


### Features

* Spielplan-Abgleich über täglichen GitHub-Actions-Relay statt Fly-internem Job ([383eab6](https://github.com/ajFourteen/Watchparty/commit/383eab6e8ff76241182f43966ed88f68aca1f8c7))

## [3.8.1](https://github.com/ajFourteen/Watchparty/compare/v3.8.0...v3.8.1) (2026-08-18)


### Bug Fixes

* automatischen Rejoin-Fehlschlag beim Start nicht als Fehler zeigen ([fe385e9](https://github.com/ajFourteen/Watchparty/commit/fe385e90e54b30ac7b5cab71086678c7167ea296))

# [3.8.0](https://github.com/ajFourteen/Watchparty/compare/v3.7.1...v3.8.0) (2026-08-18)


### Bug Fixes

* CI-Blocker beheben — Flaky-Test und fehlende Feature-Abdeckung ([e4879b2](https://github.com/ajFourteen/Watchparty/commit/e4879b2a6c4100b577a0f0dcb49de14f43a9de4f))
* docker in docker ([24fbb76](https://github.com/ajFourteen/Watchparty/commit/24fbb76ee4a5026fd65e88977e70bb6b4f616edf))


### Features

* Alarm-Mail bei andauerndem Feed-Ausfall (docs/betrieb-tippspiel.md) ([299d36c](https://github.com/ajFourteen/Watchparty/commit/299d36ccef16e5a1c60bd378c52bebc15058256a))
* Handeintrag-Endpunkt für den Betreiber (Kriterium 14/13.3-h) ([793f15d](https://github.com/ajFourteen/Watchparty/commit/793f15d39aab5170127560343d5a8cf456931b60))
* Impressum und Datenschutzerklärung ins Frontend einbinden ([071c36d](https://github.com/ajFourteen/Watchparty/commit/071c36dc599ed419132fbf35a653431bb4ca1472))
* Mailversand über Strato-SMTP (Rückfrage vom 2026-08-18) ([bdd96a1](https://github.com/ajFourteen/Watchparty/commit/bdd96a165f7f5a8eab51d1522869ca2ee8e5eba4))
* Tippspiel-Liga Stufe 1 — Wertung (Scoring, GameScore, ScoreBucket, LeaguePoints) ([aeb37f2](https://github.com/ajFourteen/Watchparty/commit/aeb37f2ed3be895f8364320b121a81486f9a5304))
* Tippspiel-Liga Stufe 2 — Postgres-Anbindung, Flyway, Account-Repository ([a329d43](https://github.com/ajFourteen/Watchparty/commit/a329d43c5f1a04b5c6426bcb2aed1f9472175767))
* Tippspiel-Liga Stufe 3 — Konten (Magic Link, Sitzung, Rate Limit, Löschung) ([8631c1b](https://github.com/ajFourteen/Watchparty/commit/8631c1b3a1e3322b020452c9054d60317153b41d))
* Tippspiel-Liga Stufe 4 — Spieldaten (ESPN-Feed, Nachführ-Job, Handeintrag) ([b634c6b](https://github.com/ajFourteen/Watchparty/commit/b634c6b49a6855d25ec8a63c2382ed37d812c8c8))
* Tippspiel-Liga Stufe 5 — Tippen (Ergebnistipp, Verdeckung bis Anstoß) ([940bd00](https://github.com/ajFourteen/Watchparty/commit/940bd00d12ab464dd44add9780b4b85303324c41))
* Tippspiel-Liga Stufe 6 — Ligen (Anlegen, Beitreten, Rangliste) ([5a4517a](https://github.com/ajFourteen/Watchparty/commit/5a4517a22a3c8b73140cb3dd3a60a32c859c21c6))
* Tippspiel-Liga Stufe 7a — HTTP-Adapter (Anmeldung, Tippen, Ligen) ([1305a30](https://github.com/ajFourteen/Watchparty/commit/1305a301696b41ae9940f3401b44baeadc87dbc0))
* Tippspiel-Liga Stufe 7b — React-Frontend und Moduswechsel ([ffc3c4d](https://github.com/ajFourteen/Watchparty/commit/ffc3c4da6fc192f96b983fe6344d1aadee7149fc))

## [3.7.1](https://github.com/ajFourteen/Watchparty/compare/v3.7.0...v3.7.1) (2026-08-17)


### Bug Fixes

* Code-Feld-Beschreibung kürzen ([b13146a](https://github.com/ajFourteen/Watchparty/commit/b13146ac4bd65c68ec1c728c4a400aa5a7775a91))
* Mitspielen-Knopf erst bei vollstaendigem vierstelligen Code aktiv ([0bc0fda](https://github.com/ajFourteen/Watchparty/commit/0bc0fdaf00e69dfc5f951439ba829f0dcaf3aa19))

# [3.7.0](https://github.com/ajFourteen/Watchparty/compare/v3.6.1...v3.7.0) (2026-08-17)


### Features

* mehrere Watchpartys gleichzeitig (Feature 004) ([526d619](https://github.com/ajFourteen/Watchparty/commit/526d619f94cae3670d77f3b603ec673e58485180))

## [3.6.1](https://github.com/ajFourteen/Watchparty/compare/v3.6.0...v3.6.1) (2026-08-12)


### Bug Fixes

* Strafenanzeige beim Aufdecken auf Kontostand kappen ([57cbb2a](https://github.com/ajFourteen/Watchparty/commit/57cbb2a9b0e318197dfab1db395a3b4f264180fa))

# [3.6.0](https://github.com/ajFourteen/Watchparty/compare/v3.5.0...v3.6.0) (2026-08-11)


### Features

* Fenster schließt, sobald alle getippt haben (Feature 003) ([7a36608](https://github.com/ajFourteen/Watchparty/commit/7a3660894a970f8e08a6b1ac3ddb1eca0a79f073))

# [3.5.0](https://github.com/ajFourteen/Watchparty/compare/v3.4.0...v3.5.0) (2026-08-11)


### Features

* Oberfläche und Bedienung überarbeitet (Feature 002) ([4878005](https://github.com/ajFourteen/Watchparty/commit/487800595e41efb297cc0a3f6631f7f26a199aa4))
* Parameter im WELCOME und Teilnehmer ohne Tipp im Zustand ([cc3275f](https://github.com/ajFourteen/Watchparty/commit/cc3275f7df24603f5cd05187ff9a24f6e366ab1f))

# [3.4.0](https://github.com/ajFourteen/Watchparty/compare/v3.3.0...v3.4.0) (2026-08-09)


### Features

* Screen Wake Lock gegen unbemerktes Wandern der Host-Rolle ([ea5b3c1](https://github.com/ajFourteen/Watchparty/commit/ea5b3c16efe3bf30c668c77351f121442205330b))

# [3.3.0](https://github.com/ajFourteen/Watchparty/compare/v3.2.0...v3.3.0) (2026-08-06)


### Bug Fixes

* ArchUnit-Tests liefen nie mit + PIT-Guard-Test fuer HIGH-Klassen ([e3b76cd](https://github.com/ajFourteen/Watchparty/commit/e3b76cd1a4a2c2ec9a7adb6205aeeeb67b38126a))
* toten Nullcheck in RoomView.deltas() entfernt ([503d019](https://github.com/ajFourteen/Watchparty/commit/503d019a57504e380cc8cafcd859544ab42ac559))


### Features

* Ausnahmenregister fuer Mutationstests (docs/test-ausnahmen.md) ([30ca726](https://github.com/ajFourteen/Watchparty/commit/30ca726a864b1b2c2dd4adc26ffdd4e8a89fc44e))
* Ebenen-Disjunktheit als automatisiertes Gate ([48a7e5d](https://github.com/ajFourteen/Watchparty/commit/48a7e5da065e8386e17ce7bdc81aa332e3d333b0))
* JGiven-Bericht auf GitHub Pages veroeffentlichen ([7610ef7](https://github.com/ajFourteen/Watchparty/commit/7610ef710bb1ca2add7c6d7038f381e8427dd570))

# [3.2.0](https://github.com/ajFourteen/Watchparty/compare/v3.1.0...v3.2.0) (2026-08-05)


### Bug Fixes

* awaitWritten wartet auf die laufende Nummer statt auf ein Idle-Flag ([6fb20a2](https://github.com/ajFourteen/Watchparty/commit/6fb20a29f77ffc47a6307aa24e3e2dcde15ec37e))


### Features

* jMolecules-Stereotypen fuer DDD-Bausteine und Onion-Ringe (ADR-027) ([53bcb07](https://github.com/ajFourteen/Watchparty/commit/53bcb0703637739fa4895f0b9ecb829637eee6ff))
* JSpecify-Nullness mit NullAway durchgesetzt (ADR-026) ([549ec68](https://github.com/ajFourteen/Watchparty/commit/549ec68345c188bc1439bf944f21fc8b39775629))

# [3.1.0](https://github.com/ajFourteen/Watchparty/compare/v3.0.0...v3.1.0) (2026-08-02)


### Bug Fixes

* Formulierung im Guide ([0560f82](https://github.com/ajFourteen/Watchparty/commit/0560f82af99762a662c241c8bfc01ce6282db972))


### Features

* Raumzustand beim Start aus dem Snapshot laden (ADR-023) ([d573763](https://github.com/ajFourteen/Watchparty/commit/d5737630a753834a625bb72d60048198745f702f))
* RESET setzt den ganzen Raum zurück (ADR-023, Abschnitt 12) ([a435c3e](https://github.com/ajFourteen/Watchparty/commit/a435c3e2f79aaaa657510d0577bb53b87aa98239))
* Snapshot-Datenmodell für den Raumzustand (ADR-023) ([0518cc4](https://github.com/ajFourteen/Watchparty/commit/0518cc4c8864b0c23fc358d3102375bfdcf204d7))
* SnapshotStore schreibt den Raumzustand bei jeder Änderung (ADR-023) ([6f51af9](https://github.com/ajFourteen/Watchparty/commit/6f51af9641ac0f37af1a0c13012b905f56146ed4))

# [3.0.0](https://github.com/ajFourteen/Watchparty/compare/v2.0.1...v3.0.0) (2026-08-01)


* feat!: Wettkatalog, Kurzanleitung und Broadcast-Look ([84292e0](https://github.com/ajFourteen/Watchparty/commit/84292e000cb16624b0a4d9551e907a5140430bcf))


### Bug Fixes

* **build:** Tests lauffähig machen und Quelltext-Kodierung festlegen ([da397f4](https://github.com/ajFourteen/Watchparty/commit/da397f469801d4630f4ff47099aad59e444cbae8))


### BREAKING CHANGES

* Das Protokoll spricht durchgehend von Wette und Tipp. Aus
`OPEN_MARKET`/`CLOSE_MARKET`/`PLACE_BET`/`YOUR_BET` wurden
`OPEN_BET`/`CLOSE_BET`/`PLACE_PICK`/`YOUR_PICK`, im STATE aus `market` `bet`,
aus `betCount` `pickCount` und aus `revealedBets` `revealedPicks`. Neu sind
`ANNUL`, der Katalog an `WELCOME` und `annulReason` im STATE. Server und
Frontend kommen aus demselben Jar (ADR-015), ein Deploy tauscht also beide
Seiten gleichzeitig; nur ein alter Client im Browser-Cache muss neu geladen
werden.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>

## [2.0.1](https://github.com/ajFourteen/Watchparty/compare/v2.0.0...v2.0.1) (2026-08-01)


### Bug Fixes

* Umlaute ([3ef9582](https://github.com/ajFourteen/Watchparty/commit/3ef9582e064741d532f66325cce7b1af92c9771e))

# [2.0.0](https://github.com/ajFourteen/Watchparty/compare/v1.0.1...v2.0.0) (2026-08-01)


* feat!: Protokoll auf den vollen Rundenablauf umgestellt ([4757be9](https://github.com/ajFourteen/Watchparty/commit/4757be97c0ad551201f004552c27fa76ca271394))


### Features

* Abrechnung als reine Funktion (Settlement, Etappe 2) ([868e9c6](https://github.com/ajFourteen/Watchparty/commit/868e9c673f05d920f95e2340ffd5b0cf9d333ad6))
* Frontend fuer den vollen Rundenablauf (Etappe 5) ([4f2e62c](https://github.com/ajFourteen/Watchparty/commit/4f2e62cb0820d9e976ab99aabe04743f054db3c8))
* Zustandsautomat und Protokoll fuer Rundenablauf (Etappe 3+4) ([cb28ce3](https://github.com/ajFourteen/Watchparty/commit/cb28ce3c40bb40624b8a6a5a46ead7f9aa9dbf44))


### BREAKING CHANGES

* HOST_ACTION und das State-Feld hostActionCount sind
ersatzlos entfallen (siehe cb28ce3). Sie waren nur der Skeleton-Beweis,
dass eine Host-Aktion serverseitig ankommt. Jeder Client muss auf die
vier echten Host-Aktionen (OPEN_MARKET, CLOSE_MARKET, RESOLVE) und das
neue STATE-Schema umgestellt werden.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>

## [1.0.1](https://github.com/ajFourteen/Watchparty/compare/v1.0.0...v1.0.1) (2026-08-01)


### Bug Fixes

* Health-Check, Rollback-Weg und Token-Ablauf dokumentieren ([3c10454](https://github.com/ajFourteen/Watchparty/commit/3c10454826e7ca61fde477add016fc17859736dd))

# 1.0.0 (2026-08-01)


### Bug Fixes

* Release-Commit von Semantic Release nicht erneut bauen lassen ([784c013](https://github.com/ajFourteen/Watchparty/commit/784c013e6bffb3a7c7fdce255f50ebe6a70b5b31))


### Features

* Deploy per Semantic Release automatisieren ([3ff2ad9](https://github.com/ajFourteen/Watchparty/commit/3ff2ad97b2383622cd1e959e073a452ef762ae0c))

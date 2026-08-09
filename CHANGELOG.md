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

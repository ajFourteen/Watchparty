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

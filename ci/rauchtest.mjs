// Rauchtest nach dem Deploy: beweist, dass der Raum-Thread lebt.
//
// Der Fly-Healthcheck fragt "GET /" ab und prueft damit nur die statische
// Auslieferung -- fly.toml sagt das selbst ausdruecklich ("Kein Nachweis, dass
// der Raum-Thread selbst noch lebt"). Ein Deploy, bei dem die Actor-Queue
// haengt, gilt dort als erfolgreich.
//
// Dieser Test geht deshalb den ganzen Weg: WebSocket-Upgrade, CREATE_ROOM ueber
// den Handler, Einreihung in RoomCommands, Bearbeitung auf dem Raum-Thread,
// WELCOME und STATE zurueck ueber die Ausgangs-Queue. Kommt das an, sind
// Invariante 1 bis 3 auf der laufenden Instanz nachgewiesen und nicht nur
// vermutet.
//
// Braucht Node >= 22 (globales WebSocket ohne Flag).
//
// Aufruf: node ci/rauchtest.mjs wss://host/ws

const url = process.argv[2];
if (!url) {
    console.error("Aufruf: node ci/rauchtest.mjs <wss://host/ws>");
    process.exit(2);
}

const FRIST_MS = 30_000;
// RoomCode (ADR-033): vier Zeichen aus 0-9/A-Z, ohne O, I und L.
const ROOM_CODE = /^[0-9ABCDEFGHJKMNPQRSTUVWXYZ]{4}$/;

const fehler = (nachricht) => {
    console.error(`✗ ${nachricht}`);
    process.exit(1);
};

const socket = new WebSocket(url);
const gesehen = [];

const frist = setTimeout(() => {
    fehler(`Keine vollstaendige Antwort innerhalb von ${FRIST_MS / 1000}s. ` +
        `Bisher empfangen: ${gesehen.join(", ") || "nichts"}`);
}, FRIST_MS);

socket.addEventListener("error", () => fehler(`Verbindung zu ${url} fehlgeschlagen.`));

socket.addEventListener("open", () => {
    console.log(`→ verbunden mit ${url}`);
    socket.send(JSON.stringify({ type: "CREATE_ROOM", name: "Rauchtest" }));
});

socket.addEventListener("message", (ereignis) => {
    let nachricht;
    try {
        nachricht = JSON.parse(ereignis.data);
    } catch {
        return fehler(`Antwort ist kein JSON: ${String(ereignis.data).slice(0, 200)}`);
    }
    gesehen.push(nachricht.type);

    if (nachricht.type === "WELCOME") {
        if (!ROOM_CODE.test(nachricht.roomCode ?? "")) {
            return fehler(`WELCOME ohne gueltigen roomCode: ${JSON.stringify(nachricht.roomCode)}`);
        }
        if (!Array.isArray(nachricht.catalog) || nachricht.catalog.length === 0) {
            return fehler("WELCOME ohne Wettkatalog -- der Katalog aus Anforderung 4 fehlt.");
        }
        if (typeof nachricht.params?.minStake !== "number") {
            return fehler("WELCOME ohne params.minStake (Anforderung 3.1).");
        }
        console.log(`→ WELCOME: Watchparty ${nachricht.roomCode}, ` +
            `${nachricht.catalog.length} Wetten, Mindesteinsatz ${nachricht.params.minStake}`);
        return;
    }

    if (nachricht.type === "STATE") {
        if (!gesehen.includes("WELCOME")) {
            return fehler("STATE kam vor WELCOME.");
        }
        if (nachricht.phase !== "IDLE") {
            return fehler(`Frisch angelegte Watchparty ist nicht IDLE, sondern ${nachricht.phase}.`);
        }
        console.log(`→ STATE: Phase ${nachricht.phase}, ${nachricht.players?.length ?? 0} Teilnehmer`);
        clearTimeout(frist);
        socket.close();
        console.log("✓ Rauchtest bestanden: der Raum-Thread hat geantwortet.");
        process.exit(0);
    }
});

/** Rangliste einer Liga (Saison oder Spieltag) — geteilt zwischen LeagueDetailScreen und ReportScreen (Feature 007). */
export function StandingsTable({ entries }) {
  if (entries.length === 0) {
    return <p className="hint">Noch keine gewerteten Spiele.</p>;
  }
  return (
    <ol className="roster">
      {entries.map((entry) => (
        <li key={entry.displayName} className={`row${entry.isSelf ? " self" : ""}`}>
          <span className="rank">{entry.rank}</span>
          <span className="name">{entry.displayName}</span>
          <span className="points">{entry.totalPoints}</span>
        </li>
      ))}
    </ol>
  );
}

import { twMerge } from "tailwind-merge";
// Local safe score calculator (tránh phụ thuộc server-side)
function calcScoreLocal(p: any) {
  const n = (v: any) => (Number.isFinite(Number(v)) ? Number(v) : 0);
  return n(p.kills) * 10 + Math.floor(n(p.damageDealt) / 5) + n(p.wavesSurvived) * 50 - n(p.deaths) * 20;
}
import { useGameStateSelector } from "../../lib/gameState/gameStateStore";
import { useUIStore } from "./uiStore";
import { useIsKeyDown } from "../../lib/useControls";
import { useEffect } from "react";

export function LeaderBoard({ gameOver }: { gameOver: boolean }) {
  const { leaderboardOpen, setLeaderboardOpen } = useUIStore();
  const players = useGameStateSelector((s) => s.players);
  const keyDown = useIsKeyDown("tab");
  useEffect(() => {
    setLeaderboardOpen(keyDown || gameOver);
  }, [gameOver, keyDown, setLeaderboardOpen]);

  return (
    <div
      className={twMerge(
        "flex justify-center items-center mt-28",
        !leaderboardOpen && "hidden"
      )}
    >
      <div className="flex flex-col items-center app-card">
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Kills</th>
              <th>Deaths</th>
              <th>Accuracy</th>
              <th>Waves Survived</th>
              <th>Damage</th>
              <th>Score</th>
            </tr>
          </thead>
          <tbody>
            {players &&
              Array.from(players.values())
                .map((player: any) => ({
                  ...player,
                  kills: Number.isFinite(Number(player?.kills)) ? Number(player.kills) : 0,
                  deaths: Number.isFinite(Number(player?.deaths)) ? Number(player.deaths) : 0,
                  accuracy: Number.isFinite(Number(player?.accuracy)) ? Number(player.accuracy) : 0,
                  wavesSurvived: Number.isFinite(Number(player?.wavesSurvived)) ? Number(player.wavesSurvived) : 0,
                  damageDealt: Number.isFinite(Number(player?.damageDealt)) ? Number(player.damageDealt) : 0,
                }))
                .map((player: any) => ({
                  ...player,
                  score: calcScoreLocal(player),
                }))
                .sort((a, b) => b.score - a.score)
                .map((player) => (
                  <tr key={player.sessionId} className="last:border-0">
                    <td className="uppercase">{player.name}</td>
                    <td>{String(player.kills)}</td>
                    <td>{String(player.deaths)}</td>
                    <td>{String(player.accuracy)}</td>
                    <td>{String(player.wavesSurvived)}</td>
                    <td>{String(player.damageDealt)}</td>
                    <td>{String(player.score)}</td>
                  </tr>
                ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

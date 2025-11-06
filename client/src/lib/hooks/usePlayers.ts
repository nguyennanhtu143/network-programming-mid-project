import { useGameStateSelector } from "../gameState/gameStateStore";
import { PlayerHealthState } from "../../types/gameState";

export function usePlayers() {
  const playerMap = useGameStateSelector((s) => s.players);
  return playerMap ? Array.from(playerMap.values()) : [];
}

export function useAlivePlayers() {
  const players = usePlayers();
  return players.filter(
    (player) => player.healthState == PlayerHealthState.ALIVE
  );
}

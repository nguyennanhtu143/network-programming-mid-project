import { create } from "zustand";
import { MyRoomState, PlayerState } from "../../types/gameState";

interface GameStateStore {
  gameState: MyRoomState | null;
  setState: (state: MyRoomState) => void;
  updateState: (updates: Partial<MyRoomState>) => void;
  resetState: () => void;
}

export const useGameStateStore = create<GameStateStore>((set) => ({
  gameState: null,
  setState: (state) => {
    set((prev) => {
      // Merge instead of replace to preserve references and reduce jitter
      if (!prev.gameState) {
        // First time: convert and set
        if (state.players && !(state.players instanceof Map)) {
          state.players = new Map(Object.entries(state.players));
        }
        return { gameState: state };
      }
      
      // Merge: preserve existing Map references where possible
      const existing = prev.gameState;
      const newState = { ...existing };
      
      // Merge players Map: update existing entries, keep references
      if (state.players) {
        const newPlayers = state.players instanceof Map 
          ? state.players 
          : new Map(Object.entries(state.players));
        
        // If we have existing players Map, merge into it to preserve references
        if (existing.players instanceof Map) {
          // Update existing entries
          for (const [id, player] of newPlayers.entries()) {
            const existingPlayer = existing.players.get(id);
            if (existingPlayer) {
              // Merge player data to preserve reference
              Object.assign(existingPlayer, player);
            } else {
              existing.players.set(id, player);
            }
          }
          // Remove players that no longer exist
          for (const id of existing.players.keys()) {
            if (!newPlayers.has(id)) {
              existing.players.delete(id);
            }
          }
          newState.players = existing.players; // Keep reference
        } else {
          newState.players = newPlayers;
        }
      }
      
      // Merge other fields
      if (state.zombies) newState.zombies = state.zombies;
      if (state.bullets) newState.bullets = state.bullets;
      if (state.waveInfo) newState.waveInfo = { ...existing.waveInfo, ...state.waveInfo };
      if (state.mapId) newState.mapId = state.mapId;
      if (state.gameTick !== undefined) newState.gameTick = state.gameTick;
      if (state.isGameOver !== undefined) newState.isGameOver = state.isGameOver;
      
      return { gameState: newState };
    });
  },
  updateState: (updates) =>
    set((prev) => {
      if (!prev.gameState) return prev;
      const newState = { ...prev.gameState, ...updates };
      // Convert Map from JSON if needed
      if (newState.players && !(newState.players instanceof Map)) {
        newState.players = new Map(Object.entries(newState.players));
      }
      return { gameState: newState };
    }),
  resetState: () => set({ gameState: null }),
}));

// Convenience hooks
export function useGameState(): MyRoomState | null {
  return useGameStateStore((state) => state.gameState);
}

export function usePlayers(): Map<string, PlayerState> | null {
  return useGameStateStore((state) => state.gameState?.players || null);
}

export function useZombies() {
  return useGameStateStore((state) => state.gameState?.zombies || []);
}

export function useBullets() {
  return useGameStateStore((state) => state.gameState?.bullets || []);
}

export function useWaveInfo() {
  return useGameStateStore((state) => state.gameState?.waveInfo);
}

export function useMapId(): string | null {
  return useGameStateStore((state) => state.gameState?.mapId || null);
}

// Selector hook (similar to useColyseusState)
export function useGameStateSelector<T>(
  selector: (state: MyRoomState) => T
): T | null {
  const gameState = useGameState();
  return gameState ? selector(gameState) : null;
}





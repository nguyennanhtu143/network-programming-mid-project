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
    // Convert Map from JSON if needed
    if (state.players && !(state.players instanceof Map)) {
      state.players = new Map(Object.entries(state.players));
    }
    set({ gameState: state });
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





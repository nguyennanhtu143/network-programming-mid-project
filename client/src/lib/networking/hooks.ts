import { useEffect, useRef } from "react";
import {
  websocketClient,
  useWebSocketRoom,
  setCurrentRoom,
} from "../../websocket/websocketClient";
import { useGameStateStore, useGameStateSelector } from "../gameState/gameStateStore";
import { PlayerState } from "../../types/gameState";
import { useCharacterCustomizationStore } from "../../components/ui/characterCustomizationStore";
import axios from "axios";

// const networkTickRate = 20;

export function useNetworkTick(callback: (tick: number) => void) {
  useRoomMessageHandler("gameTick", (message: number) => {
    callback(message);
  });
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const roomMessageHandlers = new Map<string, Set<(message: any) => void>>();

/**
 * This hook should only be mounted once.
 */
let currentListenerId = 0;
export function useBroadcastRoomMessages() {
  const room = useWebSocketRoom();

  useEffect(() => {
    const listenerId = ++currentListenerId;
    
    // Subscribe to all message types
    const unsubscribeHandlers: (() => void)[] = [];
    
    // Subscribe to state updates
    const unsubscribeState = websocketClient.on("stateUpdate", (state) => {
      if (listenerId !== currentListenerId) {
        return;
      }
      useGameStateStore.getState().setState(state);
      const handlers = roomMessageHandlers.get("stateUpdate") ?? new Set();
      handlers.forEach((handler) => handler(state));
    });
    unsubscribeHandlers.push(unsubscribeState);

    // Subscribe to game messages
    const unsubscribeGame = websocketClient.on("gameMessage", (message: { type: string; data: any }) => {
      if (listenerId !== currentListenerId) {
        return;
      }
      const handlers = roomMessageHandlers.get(message.type) ?? new Set();
      handlers.forEach((handler) => handler(message.data));
    });
    unsubscribeHandlers.push(unsubscribeGame);

    // Subscribe to all other events
    const eventTypes = [
      "gameTick",
      "chatMessage",
      "waveStart",
      "waveEnd",
      "gameOver",
      "playerDied",
      "playerRevived",
      "playerHurt",
      "zombieHit",
      "zombieDead",
      "blood",
      "shotSound",
      "requestSpawn",
      "requestSpawnZombie",
    ];

    eventTypes.forEach((eventType) => {
      const unsubscribe = websocketClient.on(eventType, (data) => {
        if (listenerId !== currentListenerId) {
          return;
        }
        const handlers = roomMessageHandlers.get(eventType) ?? new Set();
        handlers.forEach((handler) => handler(data));
      });
      unsubscribeHandlers.push(unsubscribe);
    });

    return () => {
      unsubscribeHandlers.forEach((unsubscribe) => unsubscribe());
    };
  }, [room]);
}

export function useRoomMessageHandler(
  type: string,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  callback: (message: any) => void
) {
  const callbackRef = useRef(callback);
  callbackRef.current = callback;

  useEffect(() => {
    const existingHandlers = roomMessageHandlers.get(type) ?? new Set();
    const handler = (message: unknown) => {
      callbackRef.current(message);
    };
    existingHandlers.add(handler);
    roomMessageHandlers.set(type, existingHandlers);

    return () => {
      const existingHandlers = roomMessageHandlers.get(type) ?? new Set();
      existingHandlers.delete(handler);
      roomMessageHandlers.set(type, existingHandlers);
    };
  }, [type]);
}

export function useSelf(): PlayerState | null {
  const sessionId = useWebSocketRoom()?.sessionId;
  const players = useGameStateSelector((s) => s.players);
  if (!sessionId || !players) return null;
  return players.get(sessionId) || null;
}

export function useSetQueryOrReconnectToken() {
  const room = useWebSocketRoom();
  useEffect(() => {
    if (!room?.id) return;
    // Set the ?roomId= query param
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.set("roomId", room.id);
    window.history.replaceState(
      {},
      "",
      `${window.location.pathname}?${urlParams}`
    );
    const reconnectToken = localStorage.getItem("reconnectToken");
    if (reconnectToken) {
      localStorage.setItem("reconnectToken", reconnectToken);
    }
  }, [room?.id]);
}

let connecting = false;

// API client for REST calls
const apiClient = axios.create({
  baseURL: process.env.NODE_ENV !== "production" 
    ? "http://localhost:8081/api" 
    : `${window.location.origin}/api`,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add auth token to requests
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("authToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function useTryJoinByQueryOrReconnectToken() {
  const room = useWebSocketRoom();
  const { selectedClass, name } = useCharacterCustomizationStore();

  useEffect(() => {
    if (connecting) return;
    if (room) return;

    // Ensure WebSocket is connected
    if (!websocketClient.isConnected()) {
      websocketClient.connect().catch(console.error);
      return;
    }

    const roomId = new URLSearchParams(window.location.search).get("roomId");
    const reconnectToken = localStorage.getItem("reconnectToken");
    
    if (reconnectToken) {
      console.log("trying to reconnect");
      connecting = true;
      websocketClient
        .reconnect(reconnectToken)
        .then((state) => {
          useGameStateStore.getState().setState(state);
          setCurrentRoom({ state });
          console.log("reconnected");
        })
        .catch(console.error)
        .finally(() => {
          connecting = false;
        });
    } else if (roomId) {
      console.log("trying to join by roomId in query", roomId);
      connecting = true;
      websocketClient
        .joinRoom(roomId.toLowerCase(), {
          playerName: name,
          playerClass: selectedClass,
        })
        .then((state) => {
          useGameStateStore.getState().setState(state);
          setCurrentRoom({ state });
        })
        .catch(console.error)
        .finally(() => {
          connecting = false;
        });
    }
  }, [room, name, selectedClass]);
}

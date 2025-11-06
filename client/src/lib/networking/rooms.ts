/* eslint-disable @typescript-eslint/no-explicit-any */
import { MyRoomState } from "../../types/gameState";
import { websocketClient, setCurrentRoom } from "../../websocket/websocketClient";
import { useGameStateStore } from "../gameState/gameStateStore";
import { useCharacterCustomizationStore } from "../../components/ui/characterCustomizationStore";
import axios from "axios";

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

export function useRoomMethods() {
  const playerOptions = useCharacterCustomizationStore();
  const roomOptsBase = {
    playerName: playerOptions.name,
    playerClass: playerOptions.selectedClass,
  };

  return {
    async createRoom(opts: any) {
      if (connecting) {
        return;
      }
      connecting = true;
      try {
        // Ensure WebSocket is connected
        if (!websocketClient.isConnected()) {
          await websocketClient.connect();
        }
        
        // Create room via REST API or WebSocket
        const state = await websocketClient.createRoom({
          ...roomOptsBase,
          ...opts,
        });
        
        useGameStateStore.getState().setState(state);
        setCurrentRoom({ state });
      } catch (error) {
        console.error("Failed to create room:", error);
      } finally {
        connecting = false;
      }
    },
    async quickPlay() {
      if (connecting) {
        console.log("QuickPlay: Already connecting, skipping...");
        return;
      }
      connecting = true;
      console.log("QuickPlay: Starting...");
      try {
        // Ensure WebSocket is connected
        if (!websocketClient.isConnected()) {
          console.log("QuickPlay: WebSocket not connected, connecting...");
          await websocketClient.connect();
          console.log("QuickPlay: WebSocket connected successfully");
        } else {
          console.log("QuickPlay: WebSocket already connected");
        }
        
        // Try to join existing room or create new one
        // For quick play, we'll try to join/create via WebSocket
        console.log("QuickPlay: Creating room with options:", {
          ...roomOptsBase,
          quickPlay: true,
        });
        const state = await websocketClient.createRoom({
          ...roomOptsBase,
          quickPlay: true,
        });
        
        console.log("QuickPlay: Room created successfully, state:", state);
        useGameStateStore.getState().setState(state);
        setCurrentRoom({ state });
      } catch (error) {
        console.error("QuickPlay: Failed to quick play:", error);
        if (error instanceof Error) {
          console.error("Error message:", error.message);
          console.error("Error stack:", error.stack);
        }
      } finally {
        connecting = false;
      }
    },
    async singlePlayer() {
      if (connecting) {
        return;
      }
      connecting = true;
      try {
        // Ensure WebSocket is connected
        if (!websocketClient.isConnected()) {
          await websocketClient.connect();
        }
        
        const state = await websocketClient.createRoom({
          ...roomOptsBase,
          singlePlayer: true,
        });
        
        useGameStateStore.getState().setState(state);
        setCurrentRoom({ state });
      } catch (error) {
        console.error("Failed to create single player room:", error);
      } finally {
        connecting = false;
      }
    },
  };
}

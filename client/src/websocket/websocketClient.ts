import { useState, useEffect, useCallback } from "react";
import { io, Socket } from "socket.io-client";
import { MyRoomState } from "../types/gameState";

// Configuration
const WS_URL =
  process.env.NODE_ENV !== "production"
    ? "http://localhost:9090"
    : window.location.origin;

// State management
let socket: Socket | null = null;
let currentRoomId: string | null = null;
let reconnectToken: string | null = null;

// Message handlers registry
const messageHandlers = new Map<string, Set<(data: any) => void>>();

// Room state
let currentRoom: {
  id: string;
  sessionId: string | null;
  state: MyRoomState | null;
} | null = null;

// Exported client
export const websocketClient = {
  // Connect to WebSocket server
  connect: (url: string = WS_URL): Promise<void> => {
    return new Promise((resolve, reject) => {
      if (socket?.connected) {
        resolve();
        return;
      }

      const token = localStorage.getItem("authToken");

      socket = io(url, {
        transports: ["websocket"],
        auth: {
          token: token || undefined,
        },
        reconnection: true,
        reconnectionDelay: 1000,
        reconnectionAttempts: 5,
      });

      socket.on("connect", () => {
        console.log("WebSocket connected");
        resolve();
      });

      socket.on("connect_error", (error) => {
        console.error("WebSocket connection error:", error);
        reject(error);
      });

      socket.on("disconnect", (reason) => {
        console.log("WebSocket disconnected:", reason);
        currentRoomId = null;
        currentRoom = null;
      });

      // Handle game state updates
      socket.on("stateUpdate", (state: MyRoomState) => {
        console.debug("[WS<-] stateUpdate", {
          players: state?.players ? (state.players instanceof Map ? state.players.size : Object.keys(state.players as any).length) : 0,
          gameTick: (state as any)?.gameTick,
          waveInfo: (state as any)?.waveInfo,
        });
        if (currentRoom) {
          currentRoom.state = state;
        }
        // Convert Map from JSON if needed
        if (state.players && !(state.players instanceof Map)) {
          state.players = new Map(Object.entries(state.players));
        }
        // Ensure arrays for bullets/zombies
        const anyState: any = state as any;
        if (!Array.isArray(anyState.zombies)) {
          anyState.zombies = Array.isArray(anyState.zombies)
            ? anyState.zombies
            : Object.values(anyState.zombies || {});
        }
        if (!Array.isArray(anyState.bullets)) {
          anyState.bullets = Array.isArray(anyState.bullets)
            ? anyState.bullets
            : Object.values(anyState.bullets || {});
        }
        // Notify all state subscribers
        messageHandlers.get("stateUpdate")?.forEach((handler) => handler(state));
      });

      // Handle game messages
      socket.on("gameMessage", (message: { type: string; data: any }) => {
        const handlers = messageHandlers.get(message.type);
        handlers?.forEach((handler) => handler(message.data));
      });

      // Handle room events
      socket.on("roomJoined", (data: {
        roomId: string;
        sessionId: string;
        reconnectToken: string;
        state: MyRoomState;
      }) => {
        console.debug("[WS<-] roomJoined", { roomId: data?.roomId, sessionId: data?.sessionId });
        if (!data || !data.roomId || !data.state) {
          console.error("[WS<-] roomJoined: Invalid data received", data);
          return;
        }
        currentRoomId = data.roomId;
        reconnectToken = data.reconnectToken;
        localStorage.setItem("reconnectToken", reconnectToken);
        currentRoom = {
          id: data.roomId,
          sessionId: data.sessionId,
          state: data.state,
        };
        // Convert Map from JSON if needed
        if (data.state && data.state.players && !(data.state.players instanceof Map)) {
          data.state.players = new Map(Object.entries(data.state.players));
        }
        // Ensure arrays for bullets/zombies on initial state
        const anyState2: any = data.state as any;
        if (!Array.isArray(anyState2.zombies)) {
          anyState2.zombies = Array.isArray(anyState2.zombies)
            ? anyState2.zombies
            : Object.values(anyState2.zombies || {});
        }
        if (!Array.isArray(anyState2.bullets)) {
          anyState2.bullets = Array.isArray(anyState2.bullets)
            ? anyState2.bullets
            : Object.values(anyState2.bullets || {});
        }
        messageHandlers.get("stateUpdate")?.forEach((handler) => handler(data.state));
      });

      // Handle all other message types
      socket.onAny((eventName, ...args) => {
        try {
          console.debug("[WS<-]", eventName, args[0]);
        } catch {}
        const handlers = messageHandlers.get(eventName);
        handlers?.forEach((handler) => handler(args[0]));
      });
    });
  },

  // Join a room
  joinRoom: async (roomId: string, options: any): Promise<MyRoomState> => {
    return new Promise((resolve, reject) => {
      if (!socket?.connected) {
        reject(new Error("Not connected"));
        return;
      }

      socket.emit("joinRoom", { roomId, ...options }, (response: any) => {
        if (!response) {
          console.error("joinRoom: No response received from server");
          reject(new Error("No response from server"));
          return;
        }
        if (response.success) {
          currentRoomId = roomId;
          reconnectToken = response.reconnectToken;
          localStorage.setItem("reconnectToken", reconnectToken);
          const state = response.state;
          if (!state) {
            console.error("joinRoom: No state in response", response);
            reject(new Error("Invalid response: missing state"));
            return;
          }
          // Convert Map from JSON if needed
          if (state.players && !(state.players instanceof Map)) {
            state.players = new Map(Object.entries(state.players));
          }
          currentRoom = {
            id: roomId,
            sessionId: response.sessionId,
            state: state,
          };
          resolve(state);
        } else {
          reject(new Error(response.error || "Failed to join room"));
        }
      });
    });
  },

  // Create a room
  createRoom: async (options: any): Promise<MyRoomState> => {
    return new Promise((resolve, reject) => {
      if (!socket?.connected) {
        console.error("createRoom: Socket not connected");
        reject(new Error("Not connected"));
        return;
      }

      console.log("createRoom: Emitting createRoom event with options:", options);
      
      // Set timeout for response (10 seconds)
      const timeout = setTimeout(() => {
        console.error("createRoom: Timeout waiting for server response");
        reject(new Error("Server response timeout"));
      }, 10000);

      socket.emit("createRoom", options, (response: any) => {
        clearTimeout(timeout);
        console.log("createRoom: Received response:", response);
        
        if (response && response.success) {
          currentRoomId = response.roomId;
          reconnectToken = response.reconnectToken;
          localStorage.setItem("reconnectToken", reconnectToken);
          const state = response.state;
          // Convert Map from JSON if needed
          if (state.players && !(state.players instanceof Map)) {
            state.players = new Map(Object.entries(state.players));
          }
          currentRoom = {
            id: response.roomId,
            sessionId: response.sessionId,
            state: state,
          };
          console.log("createRoom: Successfully created room", response.roomId);
          resolve(state);
        } else {
          const errorMsg = response?.error || "Failed to create room";
          console.error("createRoom: Server returned error:", errorMsg);
          reject(new Error(errorMsg));
        }
      });
    });
  },

  // Reconnect to room
  reconnect: async (token: string): Promise<MyRoomState> => {
    return new Promise((resolve, reject) => {
      if (!socket?.connected) {
        reject(new Error("Not connected"));
        return;
      }

      socket.emit("reconnect", { token }, (response: any) => {
        if (response.success) {
          currentRoomId = response.roomId;
          reconnectToken = response.reconnectToken;
          localStorage.setItem("reconnectToken", reconnectToken);
          const state = response.state;
          // Convert Map from JSON if needed
          if (state.players && !(state.players instanceof Map)) {
            state.players = new Map(Object.entries(state.players));
          }
          currentRoom = {
            id: response.roomId,
            sessionId: response.sessionId,
            state: state,
          };
          resolve(state);
        } else {
          reject(new Error(response.error || "Failed to reconnect"));
        }
      });
    });
  },

  // Send game message
  send: (type: string, message: any) => {
    if (!socket?.connected) {
      console.warn("Cannot send message: not connected");
      return;
    }
    socket.emit("gameMessage", { type, data: message });
  },

  // Subscribe to message type
  on: (type: string, callback: (data: any) => void) => {
    const handlers = messageHandlers.get(type) || new Set();
    handlers.add(callback);
    messageHandlers.set(type, handlers);

    // Return unsubscribe function
    return () => {
      const handlers = messageHandlers.get(type);
      handlers?.delete(callback);
      if (handlers?.size === 0) {
        messageHandlers.delete(type);
      }
    };
  },

  // Unsubscribe from message type
  off: (type: string, callback?: (data: any) => void) => {
    if (callback) {
      const handlers = messageHandlers.get(type);
      handlers?.delete(callback);
    } else {
      messageHandlers.delete(type);
    }
  },

  // Get current state
  getState: (): MyRoomState | null => {
    return currentRoom?.state || null;
  },

  // Get current room ID
  getRoomId: (): string | null => {
    return currentRoomId;
  },

  // Get session ID
  getSessionId: (): string | null => {
    return currentRoom?.sessionId || socket?.id || null;
  },

  // Check if connected
  isConnected: (): boolean => {
    return socket?.connected || false;
  },

  // Disconnect
  disconnect: () => {
    socket?.disconnect();
    socket = null;
    currentRoomId = null;
    reconnectToken = null;
    currentRoom = null;
    messageHandlers.clear();
  },
};

// React hooks for compatibility
export function useWebSocketRoom() {
  const [room, setRoom] = useState<{
    id: string | null;
    sessionId: string | null;
    send: (type: string, message: any) => void;
    on: (type: string, callback: (data: any) => void) => () => void;
    off: (type: string, callback?: (data: any) => void) => void;
  } | null>(null);

  useEffect(() => {
    const updateRoom = () => {
      setRoom({
        id: websocketClient.getRoomId(),
        sessionId: websocketClient.getSessionId(),
        send: websocketClient.send,
        on: websocketClient.on,
        off: websocketClient.off,
      });
    };

    updateRoom();
    const interval = setInterval(updateRoom, 100);

    return () => clearInterval(interval);
  }, []);

  return room;
}

// Compatibility exports (matching old Colyseus API)
export const setCurrentRoom = (room: any) => {
  // For compatibility, but WebSocket handles this automatically
  return Promise.resolve(room);
};

export const connectToColyseus = () => {
  return websocketClient.connect();
};

export const disconnectFromColyseus = () => {
  websocketClient.disconnect();
};


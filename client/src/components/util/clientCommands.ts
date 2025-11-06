import { useCallback } from "react";
import { disconnectFromColyseus } from "../../websocket/websocketClient";
import { useGameStateSelector } from "../../lib/gameState/gameStateStore";
import { useClientSettings } from "../ui/soundStore";

export function useClientCommandInterceptor(
  respond: ({ message }: { message: string; color?: string }) => void
) {
  const currentMapId = useGameStateSelector((s) => s.mapId);

  return useCallback(
    (message: string) => {
      if (!message.startsWith("/")) return message;
      const command = message.substring(1).split(" ")[0].toLowerCase();
      switch (command) {
        case "disconnect":
        case "leave":
          disconnectFromColyseus();
          break;
        case "test":
          respond({ message: "Connection Test Successful!" });
          break;
        case "showfps":
          useClientSettings.getState().setShowFps(true);
          break;
        case "verifymap":
          if (!currentMapId) {
            return respond({
              message: "You must be in a map to verify it",
              color: "red",
            });
          }
          respond({ message: "verifyMap not available on REST client yet." });
          break;
        default:
          return message;
      }
    },
    [respond, currentMapId]
  );
}

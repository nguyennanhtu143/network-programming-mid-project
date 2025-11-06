import { useLogto } from "@logto/react";
import { useEffect } from "react";
import { websocketClient } from "../../websocket/websocketClient";

export function useSetSpringAuthToken() {
  const { isAuthenticated, getAccessToken } = useLogto();

  useEffect(() => {
    (async () => {
      if (isAuthenticated) {
        const accessToken = await getAccessToken(
          "https://apocalypse.p3ntest.dev/"
        );
        localStorage.setItem("authToken", accessToken);
        
        // If WebSocket is connected, we need to reconnect with new token
        // For now, the token will be used on next connection
        // In production, you might want to disconnect and reconnect
      } else {
        localStorage.removeItem("authToken");
        // Optionally disconnect WebSocket if not authenticated
        // websocketClient.disconnect();
      }
    })();
  }, [isAuthenticated, getAccessToken]);
}

// Keep old name for backward compatibility during migration
export const useSetColyseusAuthToken = useSetSpringAuthToken;

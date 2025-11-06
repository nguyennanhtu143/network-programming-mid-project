import { useWebSocketRoom, websocketClient } from "../../websocket/websocketClient";
import { useGameStateSelector } from "../../lib/gameState/gameStateStore";
import { useEffect, useState } from "react";
type GameLevel = { objects: unknown[] };

export function useRemoteLevel(mapId: string | undefined) {
  const [data, setData] = useState<{ level: GameLevel; id: string; name: string } | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Subscribe to mapData via WebSocket (server-spring enhancement)
    const off = websocketClient.on("mapData", (map: any) => {
      if (!map) return;
      setData({ level: map.level as GameLevel, id: map.id, name: map.name });
      setLoading(false);
    });

    let aborted = false;
    if (!mapId) {
      setData(null);
      return () => {
        off?.();
      };
    }
    setLoading(true);
    // Yêu cầu server gửi lại map nếu miss sự kiện mapData ban đầu
    try {
      websocketClient.send("requestMap", { mapId });
    } catch (e) {}
    return () => {
      aborted = true;
      off?.();
    };
  }, [mapId]);

  if (!data) return null;
  return data;
}

export function useCurrentRemoteLevel() {
  const mapId = useGameStateSelector((s) => s.mapId);
  const room = useWebSocketRoom();
  const level = useRemoteLevel(mapId);

  useEffect(() => {
    if (level) {
      room.send("finishedLoading");
    }
  }, [level, room]);

  return level;
}

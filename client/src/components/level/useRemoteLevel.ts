import { useWebSocketRoom } from "../../websocket/websocketClient";
import { useGameStateSelector } from "../../lib/gameState/gameStateStore";
import { useEffect, useState } from "react";
type GameLevel = { objects: unknown[] };

export function useRemoteLevel(mapId: string | undefined) {
  const [data, setData] = useState<{ level: GameLevel; id: string; name: string } | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let aborted = false;
    if (!mapId) {
      setData(null);
      return;
    }
    setLoading(true);
    fetch(`/api/maps/${mapId}`)
      .then((r) => r.json())
      .then((res) => {
        if (!aborted) setData(res);
      })
      .catch(() => {
        if (!aborted) setData(null);
      })
      .finally(() => {
        if (!aborted) setLoading(false);
      });
    return () => {
      aborted = true;
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

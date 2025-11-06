import { useWebSocketRoom } from "../../websocket/websocketClient";
import { useRoomMessageHandler } from "../../lib/networking/hooks";
import { useSpawnPoints } from "../level/spawnPointContext";

export function PlayerSpawner() {
  const room = useWebSocketRoom();
  const spawnPoints = useSpawnPoints("player");
  useRoomMessageHandler("requestSpawn", () => {
    const fallback = { x: 400, y: 300 };
    const hasPoints = Array.isArray(spawnPoints) && spawnPoints.length > 0;
    const spawnPoint = hasPoints
      ? spawnPoints[Math.floor(Math.random() * spawnPoints.length)]
      : fallback;

    room?.send("spawnSelf", {
      x: spawnPoint.x,
      y: spawnPoint.y,
    });
  });

  return null;
}

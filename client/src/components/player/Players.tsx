import {
  PlayerHealthState,
  PlayerState,
} from "../../types/gameState";
import { useWebSocketRoom } from "../../websocket/websocketClient";
import { useGameStateSelector } from "../../lib/gameState/gameStateStore";
import { Container, Sprite, useTick } from "@pixi/react";
import { useLerped, useLerpedRadian } from "../../lib/useLerped";
import { PlayerSprite } from "./PlayerSprite";
import { PlayerSelf } from "./PlayerSelf";
import Matter, { Body } from "matter-js";
import { useBodyRef } from "../../lib/physics/hooks";
import { SpectateControls } from "./SpectateControls";
import { useState, useRef, useEffect } from "react";
import { useRoomMessageHandler, useSelf } from "../../lib/networking/hooks";
import { playSelfDied } from "../../lib/sound/sound";
import { Texture } from "pixi.js";
import { getMaxHealth } from "../../../../server/src/game/player";
import { getEntityFilters } from "../graphics/filters";

export function Players() {
  const players = useGameStateSelector((s) => s.players);
  const self = useSelf();

  useRoomMessageHandler("playerDied", (message) => {
    if (message.playerId === self.sessionId) {
      playSelfDied();
    }
  });

  if (!players) {
    return null;
  }

  return (
    <Container>
      {Array.from(players.entries()).map(([id, player]) => (
        <Player key={id} player={player} />
      ))}
    </Container>
  );
}

function Player({ player }: { player: PlayerState }) {
  const sessionId = useWebSocketRoom()?.sessionId;
  const isMe = player.sessionId === sessionId;

  if (isMe) {
    return player.healthState == PlayerHealthState.ALIVE ? (
      <PlayerSelf player={player} />
    ) : (
      <>
        <SpectateControls x={player.x} y={player.y} />
        {PlayerHealthState.DEAD && <PlayerGrave x={player.x} y={player.y} />}
      </>
    );
  } else {
    return <OtherPlayer player={player} />;
  }
}

function OtherPlayer({ player }: { player: PlayerState }) {
  if (player.healthState === PlayerHealthState.ALIVE) {
    return <OtherAlivePlayer player={player} />;
  } else if (player.healthState === PlayerHealthState.DEAD) {
    return <PlayerGrave x={player.x} y={player.y} />;
  } else {
    return null;
  }
}

function PlayerGrave({ x, y }: { x: number; y: number }) {
  const rotation = useState(Math.random() * Math.PI * 2)[0];
  return (
    <Sprite
      anchor={[0.5, 0.5]}
      x={x}
      y={y}
      rotation={rotation}
      texture={Texture.from("dogtag.png")}
      scale={0.2}
    />
  );
}

function OtherAlivePlayer({ player }: { player: PlayerState }) {
  // Client-side prediction: calculate position from velocity only (like OtherBullet)
  const [x, setX] = useState(player.x);
  const [y, setY] = useState(player.y);
  const [rotation, setRotation] = useState(player.rotation);
  
  // Track if we've initialized from server position
  const initializedRef = useRef(false);
  const lastVelocityRef = useRef({ vx: 0, vy: 0 });
  
  // Initialize position only once when player first appears or velocity changes significantly
  useEffect(() => {
    const vx = player.velocityX || 0;
    const vy = player.velocityY || 0;
    const lastVel = lastVelocityRef.current;
    
    // If velocity changed significantly (player started/stopped moving), re-initialize position
    const velChange = Math.abs(vx - lastVel.vx) + Math.abs(vy - lastVel.vy);
    if (!initializedRef.current || velChange > 50) {
      setX(player.x);
      setY(player.y);
      setRotation(player.rotation);
      initializedRef.current = true;
      lastVelocityRef.current = { vx, vy };
    }
  }, [player.x, player.y, player.rotation, player.velocityX, player.velocityY]);
  
  const collider = useBodyRef(() => {
    return Matter.Bodies.circle(player.x, player.y, 40);
  });
  
  // Calculate position from velocity each frame (pure client-side prediction, no server sync)
  useTick((delta) => {
    if (!initializedRef.current) return;
    
    // Use velocity from server to predict movement
    const vx = player.velocityX || 0;
    const vy = player.velocityY || 0;
    
    // Update position based on velocity (delta is in seconds, typically ~0.016 for 60fps)
    setX((prevX) => prevX + vx * delta);
    setY((prevY) => prevY + vy * delta);
    
    // Smooth rotation towards server rotation
    setRotation((prevRot) => {
      const targetRot = player.rotation;
      let diff = targetRot - prevRot;
      // Normalize angle difference to [-PI, PI]
      while (diff > Math.PI) diff -= 2 * Math.PI;
      while (diff < -Math.PI) diff += 2 * Math.PI;
      // Lerp rotation
      return prevRot + diff * 0.3;
    });
  });
  
  // Update collider position
  useTick(() => {
    Body.setPosition(collider.current, {
      x,
      y,
    });
  });

  return (
    <PlayerSprite
      currentAnimation={player.currentAnimation}
      name={player.name}
      playerClass={player.playerClass}
      x={x}
      y={y}
      rotation={rotation}
      health={player.health}
      maxHealth={getMaxHealth(player)}
      velocityX={player.velocityX}
      velocityY={player.velocityY}
    />
  );
}

// Type definitions for game state (replacing Colyseus Schema)
import { PlayerClass } from "../../../server/src/game/player";
import { ZombieType } from "../../../server/src/game/zombies";
import { playerConfig } from "../../../server/src/game/config";

export const PlayerHealthState = {
  ALIVE: 0,
  DEAD: 1,
  NOT_SPAWNED: 2,
} as const;

export interface PlayerUpgradeState {
  fireRate: number;
  damage: number;
  pierce: number;
  health: number;
  speed: number;
  scope: number;
}

export interface PlayerState {
  name: string;
  sessionId: string;
  x: number;
  y: number;
  rotation: number;
  connected: boolean;
  velocityX: number;
  velocityY: number;
  health: number;
  healthState: number;
  skillPoints: number;
  playerClass: PlayerClass;
  upgrades: PlayerUpgradeState;
  kills: number;
  deaths: number;
  damageDealt: number;
  wavesSurvived: number;
  accuracy: number;
  currentAnimation: number;
  finishedLoading: boolean;
}

export interface ZombieState {
  id: number;
  x: number;
  y: number;
  rotation: number;
  playerId: string;
  health: number;
  maxHealth: number;
  targetPlayerId: string;
  lastAttackTick: number;
  attackCoolDownTicks: number;
  zombieType: ZombieType;
}

export interface BulletState {
  id: number;
  playerId: string;
  originX: number;
  originY: number;
  rotation: number;
  speed: number;
  damage: number;
  piercesLeft: number;
  knockBack: number;
}

export interface WaveInfoState {
  currentWaveNumber: number;
  active: boolean;
  nextWaveStartsInSec: number;
  totalZombies: number;
  zombiesLeft: number;
}

export interface MyRoomState {
  players: Map<string, PlayerState>;
  bullets: BulletState[];
  zombies: ZombieState[];
  gameTick: number;
  waveInfo: WaveInfoState;
  isGameOver: boolean;
  mapId: string;
}





import { useCallback, useState } from "react";
import { useRoomMethods } from "../../lib/networking/rooms";
import { useCharacterCustomizationStore } from "./characterCustomizationStore";
import { MapSelector } from "./mainMenu/MapSelector";
import { twMerge } from "tailwind-merge";
import { websocketClient } from "../../websocket/websocketClient";
import { useGameStateStore } from "../../lib/gameState/gameStateStore";

let connecting = false;

export function JoinMenu() {
  const { selectedClass, name } = useCharacterCustomizationStore();
  const roomMethods = useRoomMethods();
  const [selectedRoom, setSelectedRoom] = useState<
    "singlePlayer" | "multiPlayer"
  >("multiPlayer");
  const [step, setStep] = useState<"main" | "roomSettings">("main");

  const pressQuickPlay = useCallback(() => {
    if (connecting) return;
    connecting = true;
    roomMethods
      .quickPlay()
      .finally(() => {
        connecting = false;
      });
  }, [roomMethods]);

  if (step === "roomSettings") {
    return (
      <div className="app-card h-full p-10">
        <RoomSettings
          roomType={selectedRoom}
          onBack={() => {
            setStep("main");
          }}
        />
      </div>
    );
  }

  return (
    <div className="app-card h-full p-10">
      <div className="w-full flex flex-col items-center gap-10">
        <h3 className="app-heading">Join a game</h3>
        <div className="flex flex-col gap-4 w-80">
          <button
            className="btn btn-primary"
            disabled={connecting}
            onClick={pressQuickPlay}
          >
            QuickPlay
          </button>
          <button
            className="btn "
            disabled={connecting}
            onClick={() => {
              setStep("roomSettings");
              setSelectedRoom("singlePlayer");
            }}
          >
            Single Player
          </button>
          <button
            className="btn"
            disabled={connecting}
            onClick={() => {
              setStep("roomSettings");
              setSelectedRoom("multiPlayer");
            }}
          >
            Create Room
          </button>
          <JoinByIdField />
        </div>
      </div>
    </div>
  );
}

function JoinByIdField() {
  const [error, setError] = useState<string | null>(null);
  const { selectedClass, name } = useCharacterCustomizationStore();

  const [id, setId] = useState("");

  const pressJoin = useCallback(async () => {
    if (connecting) return;
    connecting = true;
    if (!id) {
      setError("ID is required");
      connecting = false;
      return;
    }
    try {
      if (!websocketClient.isConnected()) {
        await websocketClient.connect();
      }
      const state = await websocketClient.joinRoom(id.toLowerCase(), {
        playerName: name,
        playerClass: selectedClass,
      });
      useGameStateStore.getState().setState(state);
    } catch (e: any) {
      setError(e.message || "Failed to join room");
    } finally {
      connecting = false;
    }
  }, [id, name, selectedClass]);

  return (
    <div>
      <div className="w-full flex flex-row justify-stretch join">
        <input
          type="text"
          className="input flex-1 uppercase join-item"
          placeholder="Room ID"
          value={id}
          onChange={(e) => setId(e.target.value)}
        />
        <button
          className={twMerge("btn join-item", id && "btn-primary")}
          onClick={pressJoin}
        >
          Join
        </button>
      </div>
      {error && <div className="text-red-500">{error}</div>}
    </div>
  );
}

function RoomSettings({
  roomType,
  onBack,
}: {
  roomType: "singlePlayer" | "multiPlayer";
  onBack: () => void;
}) {
  const { selectedClass, name } = useCharacterCustomizationStore();

  const [mapId, setMapId] = useState<string>("");
  const [mapName, setMapName] = useState<string>("Random Official Map");

  const [mapSelectorOpen, setMapSelectorOpen] = useState(false);

  const roomMethods = useRoomMethods();
  const pressCreate = useCallback(() => {
    if (connecting) return;
    connecting = true;
    if (roomType === "singlePlayer") {
      roomMethods
        .singlePlayer()
        .finally(() => {
          connecting = false;
        });
    } else {
      roomMethods
        .createRoom({
          quickPlay: false,
          maxPlayers: 10,
          isPrivate: true,
          waveStartType: "playerCount",
          requiredPlayerCount: 2,
          mapId: mapId || undefined,
          playerName: name,
          playerClass: selectedClass,
        })
        .finally(() => {
          connecting = false;
        });
    }
  }, [name, selectedClass, roomType, mapId, roomMethods]);
  return (
    <div className="flex flex-col gap-4 items-start w-full">
      <h3 className="app-heading">
        {roomType == "multiPlayer" ? "Room" : "Single Player"} Settings
      </h3>

      <div className="flex flex-row items-center gap-3 w-full">
        <h4 className="app-strong font-bold text-lg flex-1">Map: {mapName}</h4>
        <button
          className="btn btn-sm room-settings-btn-small"
          onClick={() => {
            setMapSelectorOpen(true);
          }}
        >
          Select Map
        </button>
      </div>

      <button onClick={pressCreate} className="btn btn-primary w-full room-settings-btn-primary">
        Create Room
      </button>
      <button onClick={onBack} className="btn room-settings-btn-back">
        Back
      </button>
      <MapSelector
        open={mapSelectorOpen}
        onClose={() => setMapSelectorOpen(false)}
        onSelect={(mapId, mapName) => {
          setMapId(mapId);
          setMapName(mapName);
        }}
      />
    </div>
  );
}

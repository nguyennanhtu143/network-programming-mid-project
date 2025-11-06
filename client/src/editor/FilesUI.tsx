import { useState } from "react";
import { twMerge } from "tailwind-merge";
import { useEffect } from "react";
import { useEditor } from "./mapEditorStore";
import { GameLevel } from "./types/editorTypes";
import { MapPreviewRenderer } from "../components/level/LevelInstanceRenderer";

export function FileOptions() {
  const level = useEditor((state) => state.level);
  const resetLevel = useEditor((state) => state.resetLevel);
  // REST save map
  const setCurrentView = useEditor((state) => state.setCurrentView);

  return (
    <div className="dropdown px-2">
      <div tabIndex={0} role="button" className="btn btn-primary btn-sm">
        File
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="1.5em"
          height="1.5em"
          viewBox="0 0 24 24"
        >
          <path
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2.5"
            d="m7 10l5 5m0 0l5-5"
          />
        </svg>
      </div>
      <ul
        tabIndex={0}
        className="dropdown-content z-[1] menu p-2 shadow bg-base-100 rounded-box w-52 mt-5 flex flex-col gap-2"
      >
        <li>
          <button
            className="btn btn-sm btn-primary"
            onClick={async () => {
              const name = prompt("Enter a name for the map");
              await fetch(`/api/maps`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ name: name ?? "Unnamed Map", level }),
              });
            }}
          >
            Save as New Map
          </button>
        </li>
        <li>
          <button
            className="btn btn-sm btn-accent"
            onClick={() => setCurrentView("myMaps")}
          >
            My Maps
          </button>
        </li>
        <li>
          <ConfirmButton
            onClick={() => {
              resetLevel();
            }}
            className="btn btn-sm btn-error"
          >
            Reset Editor
          </ConfirmButton>
        </li>
      </ul>
    </div>
  );
}

export function MyMapsModal() {
  const [myMaps, setMyMaps] = useState<{ id: string }[] | null>(null);
  useEffect(() => {
    let aborted = false;
    fetch(`/api/maps/my`)
      .then((r) => r.json())
      .then((res) => !aborted && setMyMaps(res))
      .catch(() => !aborted && setMyMaps([] as any));
    return () => {
      aborted = true;
    };
  }, []);

  if (!open) {
    return null;
  }

  return (
    <div className="">
      <div className="p-4">
        <h2 className="text-white font-bold text-xl mb-4">My Maps</h2>
        <div className="grid grid-cols-4 gap-2">
          {myMaps?.map((map) => (
            <MapCard key={map.id} id={map.id} />
          ))}
        </div>
      </div>
    </div>
  );
}

function MapCard({ id }: { id: string }) {
  const [map, setMap] = useState<any | null>(null);
  useEffect(() => {
    let aborted = false;
    fetch(`/api/maps/my/${id}`)
      .then((r) => r.json())
      .then((res) => !aborted && setMap(res))
      .catch(() => !aborted && setMap(null));
    return () => {
      aborted = true;
    };
  }, [id]);
  const loadLevel = useEditor((state) => state.loadLevel);
  const level = useEditor((state) => state.level);

  if (!map) return;

  return (
    <div className="app-card">
      <figure className="max-h-56">
        <MapPreviewRenderer level={map.level as GameLevel} size={400} />
      </figure>
      <div className="card-body">
        <h3 className="card-title">{map.name}</h3>
        <div className="card-actions">
          <ConfirmButton
            onClick={() => {
              loadLevel(map.level);
            }}
            className="btn btn-sm"
          >
            Load
          </ConfirmButton>
          <ConfirmButton
            onClick={async () => {
              await fetch(`/api/maps/${map.id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ level }),
              });
            }}
            className="btn btn-sm"
          >
            Overwrite
          </ConfirmButton>
          <ConfirmButton
            onClick={() => {
              fetch(`/api/maps/${map.id}`, { method: "DELETE" });
            }}
            className="btn btn-sm"
          >
            Delete
          </ConfirmButton>
          <button
            className="btn btn-sm"
            onClick={async () => {
              await fetch(`/api/maps/${map.id}/publish`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ publish: !map.published }),
              });
            }}
          >
            {map.published ? "Unpublish" : "Publish"}
          </button>
        </div>
      </div>
    </div>
  );
}

export function ConfirmButton({
  children,
  onClick,
  className,
}: {
  children: React.ReactNode;
  onClick: () => void;
  className?: string;
}) {
  const [confirm, setConfirm] = useState(false);
  return (
    <button
      className={twMerge(className, "btn", confirm && "btn btn-danger")}
      onClick={() => {
        if (confirm) {
          onClick();
          setConfirm(false);
        } else {
          setConfirm(true);
          setTimeout(() => {
            setConfirm(false);
          }, 1500);
        }
      }}
    >
      {confirm ? "Confirm?" : children}
    </button>
  );
}

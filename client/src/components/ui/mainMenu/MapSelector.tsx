type MapInfo = {
  id: string;
  level: any;
  name: string;
  verified: boolean;
  published: boolean;
};
import { useEffect, useState } from "react";
import { MapPreviewRenderer } from "../../level/LevelInstanceRenderer";
import { CenteredFullScreen } from "../uiUtils";

type onSelect = (mapId: string, mapName: string) => void;

export function MapSelector({
  open,
  onClose,
  onSelect,
}: {
  open: boolean;
  onClose: () => void;
  onSelect: onSelect;
}) {
  const [maps, setMaps] = useState<{
    verifiedMaps: MapInfo[];
    myMaps?: MapInfo[];
    communityMaps: MapInfo[];
  } | null>(null);
  useEffect(() => {
    let aborted = false;
    fetch(`/api/maps/playable`)
      .then((r) => r.json())
      .then((res) => {
        if (!aborted) setMaps(res);
      })
      .catch(() => {
        if (!aborted) setMaps({ verifiedMaps: [], communityMaps: [] });
      });
    return () => {
      aborted = true;
    };
  }, []);

  if (!open) return null;
  // Show skeleton while loading
  if (!maps) {
    return (
      <CenteredFullScreen onClose={onClose}>
        <div
          className="app-card p-6 flex items-center justify-center"
          style={{ width: "60vw", maxWidth: "900px" }}
        >
          <h3 className="app-heading">Loading maps...</h3>
        </div>
      </CenteredFullScreen>
    );
  }

  const _onSelect = (mapId: string, mapName: string) => {
    onSelect(mapId, mapName);
    onClose();
  };

  return (
    <CenteredFullScreen onClose={onClose}>
      <div
        className="app-card p-6 overflow-y-auto"
        style={{
          maxHeight: "80vh",
          width: "80vw",
          maxWidth: "1200px",
        }}
      >
        <div className="flex flex-col gap-4">
          <MapsSection
            title="Official Maps"
            maps={maps.verifiedMaps as MapInfo[]}
            onSelect={_onSelect}
          />
          {maps.myMaps && maps.myMaps.length > 0 && (
            <MapsSection
              title="My Maps"
              maps={maps.myMaps as MapInfo[]}
              onSelect={_onSelect}
            />
          )}
          <MapsSection
            title="Community Maps"
            maps={maps.communityMaps as MapInfo[]}
            onSelect={_onSelect}
          />
          {maps.verifiedMaps.length === 0 && (!maps.myMaps || maps.myMaps.length === 0) && maps.communityMaps.length === 0 && (
            <div className="p-4">
              <h4 className="app-strong">No maps available.</h4>
            </div>
          )}
        </div>
      </div>
    </CenteredFullScreen>
  );
}

function MapsSection({
  title,
  maps,
  onSelect,
}: {
  title: string;
  maps: MapInfo[];
  onSelect: onSelect;
}) {
  if (maps.length === 0) return null;
  
  return (
    <div className="p-2 rounded-xl overflow-x-auto">
      <h3 className="app-heading text-2xl mb-3">{title}</h3>
      <div className="flex flex-row gap-2">
        {maps.map((map) => (
          <MapCard key={map.id} info={map} onSelect={onSelect} />
        ))}
      </div>
    </div>
  );
}

function MapCard({ info, onSelect }: { info: MapInfo; onSelect: onSelect }) {
  return (
    <div
      className="bg-base-300/50 p-4 rounded-xl transition-all hover:scale-105 cursor-pointer h-44 w-96 flex flex-row gap-4 border border-base-300"
      onClick={() => {
        onSelect(info.id, info.name);
      }}
    >
      <MapPreviewRenderer level={info.level} size={140} />
      <h4 className="app-strong font-bold text-xl max-w-sm text-ellipsis w-60 text-nowrap overflow-hidden">
        {info.name}
      </h4>
    </div>
  );
}

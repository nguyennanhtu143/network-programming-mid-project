import { useEffect, useState } from "react";

export function useCustomAssetBaseUrl() {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    let aborted = false;
    fetch("/api/assets/endpoint")
      .then((r) => r.text())
      .then((text) => {
        if (!aborted) setUrl(text);
      })
      .catch(() => {
        if (!aborted) setUrl(null);
      });
    return () => {
      aborted = true;
    };
  }, []);
  return url ?? "";
}

export function useCustomAsset(uploadId: string) {
  const baseUrl = useCustomAssetBaseUrl();
  return `${baseUrl}/asset/${uploadId}.png`;
}

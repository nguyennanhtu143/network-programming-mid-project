import React from "react";

export function CenteredFullScreen({
  children,
  onClose,
}: {
  children: React.ReactNode;
  onClose?: () => void;
}) {
  return (
    <div
      className="fixed top-0 left-0 w-screen h-screen flex items-center justify-center pointer-events-auto z-[9999] bg-black/50 backdrop-blur-sm"
      onClick={() => {
        onClose?.();
      }}
    >
      <div
        className="z-[10000]"
        onClick={(e) => {
          e.stopPropagation();
        }}
      >
        {children}
      </div>
    </div>
  );
}

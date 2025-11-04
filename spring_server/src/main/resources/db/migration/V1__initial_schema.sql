-- Initial schema for Zombies Multiplayer Game

-- Create User table
CREATE TABLE "User" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "scopePermissions" TEXT[],
    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

-- Create Map table
CREATE TABLE "Map" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "level" JSONB NOT NULL,
    "verified" BOOLEAN NOT NULL DEFAULT false,
    "published" BOOLEAN NOT NULL DEFAULT false,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,
    "authorId" TEXT NOT NULL,
    CONSTRAINT "Map_pkey" PRIMARY KEY ("id")
);

-- Create CustomAsset table
CREATE TABLE "CustomAsset" (
    "id" TEXT NOT NULL,
    "uploadId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "description" TEXT,
    "tags" TEXT[],
    "verified" BOOLEAN NOT NULL DEFAULT false,
    "uploadedById" TEXT,
    CONSTRAINT "CustomAsset_pkey" PRIMARY KEY ("id")
);

-- Create PlayedGame table
CREATE TABLE "PlayedGame" (
    "id" TEXT NOT NULL,
    "mapId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "highestWaveSurvived" INTEGER NOT NULL,
    CONSTRAINT "PlayedGame_pkey" PRIMARY KEY ("id")
);

-- Create PlayedGameParticipant table
CREATE TABLE "PlayedGameParticipant" (
    "id" TEXT NOT NULL,
    "playedGameId" TEXT NOT NULL,
    "userId" TEXT,
    "username" TEXT NOT NULL DEFAULT 'Anonymous',
    "kills" INTEGER NOT NULL,
    "deaths" INTEGER NOT NULL,
    "accuracy" DOUBLE PRECISION NOT NULL,
    "wavesSurvived" INTEGER NOT NULL,
    "damageDealt" INTEGER NOT NULL,
    "score" INTEGER NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "PlayedGameParticipant_pkey" PRIMARY KEY ("id")
);

-- Add foreign keys
ALTER TABLE "Map" 
    ADD CONSTRAINT "Map_authorId_fkey" 
    FOREIGN KEY ("authorId") REFERENCES "User"("id") 
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "CustomAsset" 
    ADD CONSTRAINT "CustomAsset_uploadedById_fkey" 
    FOREIGN KEY ("uploadedById") REFERENCES "User"("id") 
    ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE "PlayedGame" 
    ADD CONSTRAINT "PlayedGame_mapId_fkey" 
    FOREIGN KEY ("mapId") REFERENCES "Map"("id") 
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "PlayedGameParticipant" 
    ADD CONSTRAINT "PlayedGameParticipant_playedGameId_fkey" 
    FOREIGN KEY ("playedGameId") REFERENCES "PlayedGame"("id") 
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "PlayedGameParticipant" 
    ADD CONSTRAINT "PlayedGameParticipant_userId_fkey" 
    FOREIGN KEY ("userId") REFERENCES "User"("id") 
    ON DELETE SET NULL ON UPDATE CASCADE;

-- Create indexes for better query performance
CREATE INDEX "idx_map_verified" ON "Map"("verified");
CREATE INDEX "idx_map_published" ON "Map"("published");
CREATE INDEX "idx_map_author" ON "Map"("authorId");
CREATE INDEX "idx_custom_asset_uploaded_by" ON "CustomAsset"("uploadedById");
CREATE INDEX "idx_played_game_map" ON "PlayedGame"("mapId");
CREATE INDEX "idx_participant_score" ON "PlayedGameParticipant"("score" DESC);
CREATE INDEX "idx_participant_user" ON "PlayedGameParticipant"("userId");



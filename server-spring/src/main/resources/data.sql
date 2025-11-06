-- Seed data for game_server_db
-- Note: This file is executed automatically by Spring Boot when using hibernate ddl-auto: create

-- Clear existing data (optional, for clean seed)
TRUNCATE TABLE played_game_participants CASCADE;
TRUNCATE TABLE played_games CASCADE;
TRUNCATE TABLE custom_assets CASCADE;
TRUNCATE TABLE custom_asset_tags CASCADE;
TRUNCATE TABLE maps CASCADE;
TRUNCATE TABLE user_scope_permissions CASCADE;
TRUNCATE TABLE users CASCADE;

-- Insert Users
INSERT INTO users (id, name, username, password, email, user_role) VALUES
('user1', 'John Doe', 'johndoe', '$2a$10$rX8vYqZ5Q5Q5Q5Q5Q5Q5Qu5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q', 'john.doe@example.com', 'USER'),
('user2', 'Jane Smith', 'janesmith', '$2a$10$rX8vYqZ5Q5Q5Q5Q5Q5Q5Qu5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q', 'jane.smith@example.com', 'USER'),
('admin1', 'Admin User', 'admin', '$2a$10$rX8vYqZ5Q5Q5Q5Q5Q5Q5Qu5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q5Q', 'admin@example.com', 'ADMIN');

-- Insert User Scope Permissions
INSERT INTO user_scope_permissions (user_id, permission) VALUES
('admin1', 'maps:read'),
('admin1', 'maps:write'),
('admin1', 'maps:delete'),
('admin1', 'users:read'),
('admin1', 'users:write'),
('user1', 'maps:read'),
('user1', 'maps:write'),
('user2', 'maps:read');

-- Insert Maps
-- Simple JSON structure for GameLevel
INSERT INTO maps (id, name, level, verified, published, created_at, updated_at, author_id) VALUES
('map1', 'Forest Battle', '{"spawnPoints":[],"walls":[],"zombieSpawnPoints":[],"name":"Forest Battle"}', true, true, NOW(), NOW(), 'user1'),
('map2', 'Desert Arena', '{"spawnPoints":[],"walls":[],"zombieSpawnPoints":[],"name":"Desert Arena"}', true, true, NOW(), NOW(), 'user1'),
('map3', 'City Streets', '{"spawnPoints":[],"walls":[],"zombieSpawnPoints":[],"name":"City Streets"}', false, false, NOW(), NOW(), 'user2');

-- Insert PlayedGames
INSERT INTO played_games (id, map_id, created_at, highest_wave_survived) VALUES
('game1', 'map1', NOW() - INTERVAL '2 days', 5),
('game2', 'map1', NOW() - INTERVAL '1 day', 8),
('game3', 'map2', NOW() - INTERVAL '6 hours', 3),
('game4', 'map2', NOW() - INTERVAL '3 hours', 10);

-- Insert PlayedGameParticipants
INSERT INTO played_game_participants (id, played_game_id, user_id, username, kills, deaths, accuracy, waves_survived, damage_dealt, score, created_at) VALUES
('participant1', 'game1', 'user1', 'johndoe', 45, 2, 0.75, 5, 12500, 4500, NOW() - INTERVAL '2 days'),
('participant2', 'game1', 'user2', 'janesmith', 38, 3, 0.68, 5, 9800, 3800, NOW() - INTERVAL '2 days'),
('participant3', 'game2', 'user1', 'johndoe', 72, 1, 0.82, 8, 18900, 7200, NOW() - INTERVAL '1 day'),
('participant4', 'game3', 'user2', 'janesmith', 25, 5, 0.55, 3, 6200, 2500, NOW() - INTERVAL '6 hours'),
('participant5', 'game4', 'user1', 'johndoe', 95, 0, 0.88, 10, 24500, 9500, NOW() - INTERVAL '3 hours'),
('participant6', 'game4', NULL, 'Anonymous', 60, 2, 0.70, 10, 15200, 6000, NOW() - INTERVAL '3 hours');

-- Insert CustomAssets
INSERT INTO custom_assets (id, upload_id, name, description, verified, uploaded_by_id) VALUES
('asset1', 'upload1', 'Zombie Skin Pack', 'A collection of custom zombie textures', true, 'user1'),
('asset2', 'upload2', 'Desert Map Theme', 'Custom theme for desert maps', false, 'user2'),
('asset3', 'upload3', 'Weapon Skin Bundle', 'Custom weapon skins and models', true, 'user1');

-- Insert CustomAsset Tags
INSERT INTO custom_asset_tags (asset_id, tag) VALUES
('asset1', 'zombie'),
('asset1', 'texture'),
('asset1', 'skin'),
('asset2', 'map'),
('asset2', 'theme'),
('asset2', 'desert'),
('asset3', 'weapon'),
('asset3', 'skin'),
('asset3', 'bundle');


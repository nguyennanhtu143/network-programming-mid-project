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
-- Big arena with multiple tiling walls and obstacle boxes using built-in assets from client/public/assets
('map1', 'Big Arena', '{"objects":[
  {"objectType":"spawnPoint","id":"sp-player","spawns":"player","x":0,"y":0,"scale":1,"rotation":0},
  {"objectType":"spawnPoint","id":"sp-z1","spawns":"zombie","x":800,"y":0,"scale":1,"rotation":0},
  {"objectType":"spawnPoint","id":"sp-z2","spawns":"zombie","x":-800,"y":0,"scale":1,"rotation":0},

  {"objectType":"asset","id":"hwall-top","x":0,"y":-600,"scale":1,"rotation":0,
    "tiling":true,"width":3000,"height":60,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":3000,"height":60},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"hwall-bottom","x":0,"y":600,"scale":1,"rotation":0,
    "tiling":true,"width":3000,"height":60,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":3000,"height":60},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"vwall-left","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"vwall-right","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },

  {"objectType":"asset","id":"box-a","x":-600,"y":-200,"scale":1,"rotation":0,
    "tiling":false,"width":220,"height":220,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/box.jpg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":220,"height":220},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"box-b","x":0,"y":-320,"scale":1,"rotation":0,
    "tiling":false,"width":220,"height":220,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/box.jpg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":220,"height":220},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"box-c","x":700,"y":-260,"scale":1,"rotation":0,
    "tiling":false,"width":220,"height":220,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/box.jpg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":220,"height":220},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"box-d","x":-800,"y":360,"scale":1,"rotation":0,
    "tiling":false,"width":220,"height":220,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/box.jpg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":220,"height":220},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"box-e","x":0,"y":360,"scale":1,"rotation":0,
    "tiling":false,"width":220,"height":220,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/box.jpg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":220,"height":220},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"box-f","x":800,"y":360,"scale":1,"rotation":0,
    "tiling":false,"width":220,"height":220,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/box.jpg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":220,"height":220},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"diag-1","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"diag-2","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"maze-h1-left","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"maze-h1-right","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"maze-h2-left","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"maze-h2-right","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"maze-v1-top","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"maze-v1-bottom","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"maze-v2-top","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"maze-v2-bottom","x":10000,"y":10000,"scale":1,"rotation":0,
    "tiling":true,"width":1,"height":1,
    "sprite":{"assetSource":"BuiltIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1,"height":1},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"tree-1","x":-950,"y":-450,"scale":1,"rotation":0,
    "tiling":false,"width":180,"height":220,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/tree1.png"},
    "colliders":[{"x":0,"y":30,"rotation":0,"shape":{"shape":"rectangle","width":120,"height":120},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"tree-2","x":950,"y":-420,"scale":1,"rotation":0,
    "tiling":false,"width":190,"height":230,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/tree2.png"},
    "colliders":[{"x":0,"y":30,"rotation":0,"shape":{"shape":"rectangle","width":120,"height":120},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"chair-1","x":-300,"y":-350,"scale":1,"rotation":0.5,
    "tiling":false,"width":80,"height":80,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/chair.png"},
    "colliders":[{"x":0,"y":0,"rotation":0.5,"shape":{"shape":"rectangle","width":60,"height":60},"destroyBullet":false}]
  },
  {"objectType":"asset","id":"box1-1","x":-1000,"y":200,"scale":1,"rotation":0,
    "tiling":false,"width":200,"height":200,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/box1.jpg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":200,"height":200},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"box2-1","x":1000,"y":260,"scale":1,"rotation":0,
    "tiling":false,"width":200,"height":200,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/box2.png"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":200,"height":200},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"wall2-a","x":-200,"y":500,"scale":1,"rotation":0,
    "tiling":true,"width":1200,"height":50,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/wall2.png"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1200,"height":50},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"wall2-b","x":600,"y":-520,"scale":1,"rotation":0.15,
    "tiling":true,"width":900,"height":50,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/wall2.png"},
    "colliders":[{"x":0,"y":0,"rotation":0.15,"shape":{"shape":"rectangle","width":900,"height":50},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"wall3-a","x":-600,"y":-520,"scale":1,"rotation":-0.2,
    "tiling":true,"width":900,"height":50,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/wall3.png"},
    "colliders":[{"x":0,"y":0,"rotation":-0.2,"shape":{"shape":"rectangle","width":900,"height":50},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"roof1-a","x":-800,"y":100,"scale":1,"rotation":0,
    "tiling":false,"width":400,"height":300,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/roof1.png"},
    "colliders":[{"x":0,"y":40,"rotation":0,"shape":{"shape":"rectangle","width":360,"height":220},"destroyBullet":true}]
  },
  {"objectType":"asset","id":"roof2-a","x":800,"y":-100,"scale":1,"rotation":0,
    "tiling":false,"width":380,"height":280,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/editor/roff2.png"},
    "colliders":[{"x":0,"y":40,"rotation":0,"shape":{"shape":"rectangle","width":340,"height":200},"destroyBullet":true}]
  }
]}', true, true, NOW(), NOW(), 'user1'),

-- Secondary large layout (emptier arena you can extend later)
('map2', 'Open Field', '{"objects":[
  {"objectType":"spawnPoint","id":"sp-player","spawns":"player","x":-200,"y":0,"scale":1,"rotation":0},
  {"objectType":"spawnPoint","id":"sp-z","spawns":"zombie","x":200,"y":0,"scale":1,"rotation":0},
  {"objectType":"asset","id":"center-wall","x":0,"y":0,"scale":1,"rotation":0,
    "tiling":true,"width":1600,"height":40,
    "sprite":{"assetSource":"builtIn","assetPath":"/assets/sandwall.jpeg"},
    "colliders":[{"x":0,"y":0,"rotation":0,"shape":{"shape":"rectangle","width":1600,"height":40},"destroyBullet":true}]
  }
]}' , true, true, NOW(), NOW(), 'user1'),

-- Community sample (empty but valid structure)
('map3', 'Community Blank', '{"objects":[]}', false, false, NOW(), NOW(), 'user2');

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


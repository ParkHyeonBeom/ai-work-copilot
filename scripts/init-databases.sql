CREATE DATABASE IF NOT EXISTS workcopilot;
CREATE DATABASE IF NOT EXISTS workcopilot_integration;
CREATE DATABASE IF NOT EXISTS workcopilot_ai;
CREATE DATABASE IF NOT EXISTS workcopilot_briefing;
CREATE DATABASE IF NOT EXISTS workcopilot_chat;

GRANT ALL PRIVILEGES ON workcopilot.* TO 'workcopilot'@'%';
GRANT ALL PRIVILEGES ON workcopilot_integration.* TO 'workcopilot'@'%';
GRANT ALL PRIVILEGES ON workcopilot_ai.* TO 'workcopilot'@'%';
GRANT ALL PRIVILEGES ON workcopilot_briefing.* TO 'workcopilot'@'%';
GRANT ALL PRIVILEGES ON workcopilot_chat.* TO 'workcopilot'@'%';
FLUSH PRIVILEGES;

# Release Notes - AreaMonitor v2.0.2

## 🎯 Overview

AreaMonitor is a Minecraft Forge mod that allows server administrators to create monitored areas where player game modes are automatically switched when entering or leaving.

## 📦 Version 2.0.2 (2025-03-13)

### 🆕 New Features
- **Smart Message System**: Auto-detects if client has the mod installed and dynamically chooses translation method
- **Selection Tool Module**: Refactored to independent class for better maintainability
- **Thread-safe Cache**: New SmartCache class with concurrent access support

### 🐛 Bug Fixes
- **Thread Safety**: All static collections now use ConcurrentHashMap/CopyOnWriteArrayList
- **Memory Leak**: Added ServerStoppingEvent cleanup mechanism
- **Config Paths**: Use FMLPaths API for correct config file storage
- **Event Bus**: Fixed double registration, unified to annotation approach
- **Mod Icon**: Added logoFile config, fixed launcher icon display issue
- **Translation Formatting**: Fixed `{0}` placeholder not replacing, unified to `%s` format
- **Missing Translation Keys**: Added missing translation keys
- **Parameter Order**: Fixed Chinese translation parameter order errors
- **Help Command Colors**: Unified all help command colors to gold(`§6`) + gray(`§7`)
- **Coordinate Display**: Fixed incorrect coordinate display in area list and creation
- **Startup Crash**: Fixed `Registry is already frozen` error from static initialization

### ⚡ Improvements
- **Code Standards**: Defined constants to replace magic numbers
- **Exception Handling**: Added debug logging, avoid silent failures
- **i18n**: All comments and logs converted to English
- **Code Audit**: Passed Minecraft Forge 1.20.1 official development standards audit

## 📋 Requirements

- Minecraft 1.20.1
- Forge 47.4.0 or higher

## 📥 Installation

1. Install Minecraft Forge 1.20.1
2. Place the jar file in the `mods` folder
3. Start the server

## 🔧 Commands

| Command | Description |
|---------|-------------|
| `/areamonitor toggle` | Toggle monitoring on/off |
| `/areamonitor status` | Show monitoring status |
| `/areamonitor area list` | List all areas |
| `/areamonitor area create <name>` | Create a new area |
| `/areamonitor area delete <name>` | Delete an area |
| `/areamonitor area info <name>` | Show area details |
| `/areamonitor area toggle <name>` | Toggle area on/off |
| `/areamonitor visual tool` | Get selection tool |
| `/areamonitor selection create <name>` | Create area from selection |
| `/areamonitor whitelist add/remove <player>` | Manage whitelist |
| `/areamonitor language en/zh` | Switch language |

## 📄 License

MIT License
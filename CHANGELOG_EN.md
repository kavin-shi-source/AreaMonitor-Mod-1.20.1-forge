# Changelog - AreaMonitor Mod

[![中文版本](https://img.shields.io/badge/中文版本-点击这里-red.svg)](CHANGELOG.md)

## [2.0.3] - 2025-03-14

### Added
- **NBT Tag Identification**: Selection tool uses NBT tags for identification, no longer depends on name, supports any language switching

### Fixed
- **Language Switching Issue**: Fixed selection tool not working after switching language
- **Hardcoded Strings**: Removed hardcoded Chinese/English strings in SelectionTool

### Improved
- **Code Comments**: All code comments unified to English
- **Log Language**: All log outputs unified to English
- **Code Quality**: Passed final code audit, score 10/10

## [2.0.2] - 2025-03-13

### Added
- **Smart Message System**: Auto-detect client mod presence for dynamic translation
- **Selection Tool Module**: Refactored to independent class for better maintainability
- **Thread-safe Cache**: New SmartCache class with concurrent access support

### Fixed
- **Thread Safety**: All static collections use ConcurrentHashMap/CopyOnWriteArrayList
- **Memory Leak**: Added ServerStoppingEvent cleanup mechanism
- **Config Paths**: Use FMLPaths API for correct config file storage
- **Event Bus**: Fixed double registration, unified to annotation approach
- **Mod Icon**: Added logoFile config, fixed launcher icon display issue
- **Translation Formatting**: Fixed `{0}` placeholder not replacing, unified to `%s` format
- **Missing Translation Keys**: Added `command.areamonitor.area.list.header` and other missing keys
- **Parameter Order**: Fixed `blacklist.item_removed` Chinese translation parameter order
- **Help Command Colors**: Unified all help command colors to gold(`§6`) command + gray(`§7`) description
- **Coordinate Display**: Fixed incorrect coordinate display in area list and creation
- **Startup Crash**: Fixed `Registry is already frozen` error from `ItemBlacklistManager` static initialization

### Improved
- **Code Standards**: Define constants to replace magic numbers
- **Exception Handling**: Added debug logging, avoid silent failures
- **i18n**: All comments and logs converted to English
- **Code Audit**: Passed Minecraft Forge 1.20.1 official development standards audit

## [2.0.0] - 2024-03-06

### Added
- Complete Chinese/English localization
- Dynamic language switching via `/areamonitor language en/zh`
- Persistent language settings

### Fixed
- Tool names not updating on language switch
- Fallback handling for missing translation keys

### Improved
- Removed redundant code and unused imports
- Improved area checking algorithm performance

## [1.0.3] - 2024-02-15

### Added
- Debug mode logging
- `/areamonitor performance` command

### Fixed
- Area boundary detection edge cases
- Player disconnection handling

## [1.0.2] - 2024-01-30

### Added
- Spatial partitioning system
- Area boundary caching

### Fixed
- Thread safety in multiplayer
- Config file loading issues

## [1.0.1] - 2024-01-15

### Added
- Player whitelist system
- Item blacklist functionality

### Fixed
- Command registration issues
- Permission checks

## [1.0.0] - 2024-01-01

### Added
- Initial release
- Multi-area monitoring with automatic game mode switching
- Particle effect visualization
- Complete command system
- Multi-dimension support

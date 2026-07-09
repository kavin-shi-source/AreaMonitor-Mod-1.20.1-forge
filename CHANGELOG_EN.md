# Changelog - AreaMonitor Mod

[![中文版本](https://img.shields.io.io/badge/中文-版本-blue.svg)](CHANGELOG.md)

## [2.0.5] - 2025-07-10

### Teleport & Cross-Dimension
- Fixed area protection and item restrictions briefly failing after cross-dimension teleport
- Fixed chain teleport not triggering enter effects (title, game mode, triggers) at the destination area
- Fixed safe landing check not loading chunks before teleport, preventing suffocation in the Nether and falling through the world in the Overworld

### Schedule & Whitelist
- Fixed schedule auto-enable/disable logic inversion: manually disabled areas are no longer incorrectly re-enabled by the schedule
- Fixed whitelisted players still triggering leave effects (game mode switch, title, triggers) when leaving an area

### Config & Data Safety
- Config files now use atomic writes to prevent data loss on server crash
- Added circle radius cap (2000 blocks) and rectangle area cap to prevent accidental server freezes from oversized regions
- Enhanced area name validation: names with spaces, control characters, or other invalid characters are now rejected when editing config files manually
- Fixed stale data persisting in memory when config file is corrupted or missing

### Client Compatibility
- Fixed vanilla clients (without the mod) being unable to connect to the server
- Fixed server-side detection of whether a client has the mod installed
- GUI command now shows a friendly message to vanilla clients instead of sending unhandled network packets

### UI & Input
- Area name input length now matches the server-side limit (32 characters)
- Added character filtering for trigger dimension format input
- Blocked commands list now auto-lowercases to prevent case-sensitivity mismatches
- Fixed scroll panel not responding to small scroll gestures
- Input fields now filter control characters to prevent display corruption

### Stability
- Fixed multiple memory leaks for better long-term stability
- Fixed concurrent modification conflict during area saving
- Fixed network packets not properly releasing memory
- Fixed blacklist data remaining in memory after area deletion
- Proper cleanup of runtime state and async tasks on server shutdown

### Security
- Trigger configs now run full sanitization when saved from the GUI
- Expanded trigger command filter: added data/function/schedule to blocked indirect commands
- Added length limit (64 characters) for area display names

## [2.0.4] - 2025-07-04

### Added
- **GUI Management Interface**: New floating-window area management UI (Glass Morphism theme) replacing the old full-screen GUI, with dark world overlay
- **4 Sub-panels Rewrite**: AreaEditPanel (area editing with bounds type switching/protection toggles/quick link), WhitelistEditPanel (whitelist add/remove), RestrictionEditPanel (item blacklist/command restrictions), TriggerEditPanel (commands/sounds/title/teleport config)
- **GlassButton**: Unified glass-style button component used across all panels
- **Polygon Selection**: Support for 3-32 vertex polygon area selection with ray-casting collision detection
- **Area Protection System**: 6 protection types — block break/place/interact, PVP, explosion, entity damage
- **Trigger System**: Independent enter/leave trigger configs supporting commands, sounds, titles, and cross-dimension teleport
- **Template System**: 3 built-in presets (PVP Arena / Creative Zone / Adventure Zone) for one-click area creation
- **Toast Feedback**: Save/delete/create success notifications after GUI operations
- **Protection Enhancements**: Container interaction protection (chest/furnace/hopper), fluid placement protection (water/lava), item drop protection
- **Trigger Enhancements**: ActionBar messages, potion effects, cooldown timers, debounce
- **Protection Whitelist**: Per-area protection exception list — whitelisted players bypass protection rules but keep game mode switching
- **Conditional Triggers**: Trigger conditions including playerHasItem / timeRange / weather / minPlayers
- **GUI Search & Sort**: Area list fuzzy search and ascending/descending sort
- **Area Scheduling**: Auto-enable/disable areas based on in-game time, supporting cross-midnight ranges
- **Area Export/Import/Clone**: `/areamonitor area export/import/clone` commands for area data migration and duplication
- **Area Statistics**: `/areamonitor stats` showing entry count, last visitor, and last visit time per area
- **Config Backup**: `/areamonitor backup` backs up configs to config/areamonitor/backups/
- **Protection Visualization**: Red particle clusters at blocked locations on protection violation
- **Confirmation Dialogs**: ConfirmDialog component for area deletion / whitelist clearing
- **Help Tooltips**: Hover tooltips for protection types/schedule/conditions/chain
- **Conditional Activation**: Minimum online players / specific player presence activation
- **Area Chain**: Enter an area to auto-teleport to the next area in the chain

### Fixed
- Quick link command extra leading slash (sendCommand auto-adds `/`)
- WhitelistEditPanel lx local variable promoted to class field (deduplication)
- Players text Y-coordinate offset corrected
- Sub-panel onClose properly calls mainScreen.updateAfterEdit() to refresh list
- Panel close on outside click, list overflow limit, keybind conflict fixes
- ConfigManager.AreaConfig added schedule/condition/chain fields — fixed data loss after restart
- SelectionTool.createAreaFromSelection null check order — eliminated NPE risk
- C2SRequestAreaListPacket player null check on permission denial
- Removed 66 unused translation keys from zh_cn.json / en_us.json

### Improved
- Responsive layout: sub-panel controls and section backgrounds fill window width
- Unified section title bar style, title spacing, and label render alignment
- Warm Parchment theme editor config
- Full Chinese localization coverage for GUI

## [2.0.3-3] - 2025-06-19

### Refactored
- **Command System Split**: ExtendedCommands (originally 994 lines) split into 5 specialized command classes — AreaCommands, WhitelistCommands, BlacklistCommands, VisualCommands, SelectionCommands

### Improved
- **Configurable Constants**: 4 hardcoded constants moved into ConfigManager for runtime tuning without recompilation
- **Deferred IO Init**: ensureConfigFiles() moved from constructor to ServerAboutToStartEvent, eliminating file IO blocking during Forge loading

### Changed
- **AreaConfig Encapsulation**: 8 public fields changed to private + getter/setter
- **Build & Publish**: Enabled Gradle publishing config for local Maven repository

## [2.0.3-2] - 2025-06-19

### Cleanup
- **Dead Code Removal**: Removed AreaMonitor.showTitle() method and wasInArea field
- **Enum Cleanup**: Removed unused MonitorArea.BoundsType.POLYGON
- **License Unified**: README.md license badge corrected from MIT to GPL-3.0

### Improved
- **Lambda Extraction**: 7 duplicate suggests lambdas extracted into suggestAreaNames() method
- **Empty Set Reuse**: AreaManager.getCurrentAreas default value uses EMPTY_AREA_SET constant
- **Code Style**: var players replaced with explicit List<ServerPlayer> type

### Added
- **Data Class**: PlayerPosition equals()/hashCode() implementation
- **Unit Tests**: 4 core class unit tests (AreaManager / SpatialPartitionManager / AreaBounds / PlayerPosition)

## [2.0.3-1] - 2025-06-19

### Fixed
- **ConfigManager Exception Handling**: Removed throw e from static initializer — falls back to safe defaults instead of failing class loading
- **Double Particle Rendering**: Removed redundant AreaVisualizer.updatePersistentVisualizations() call in PerformanceMonitor — eliminated double particle packets per tick
- **Dimension Validation**: DimensionUtils.isValidDimension now uses ResourceLocation.tryParse() — fixed invalid formats like "foo:" passing validation
- **Item Display Name**: Fixed getItemDisplayName returning translation keys instead of actual display names
- **Whitelist Dual-Index Sync**: Whitelist storage migrated from txt to JSON (UUID→name), eliminating rename-related sync issues

### Improved
- **Whitelist Deferred Write**: dirty flag + 30-second auto-save strategy, reducing IO overhead during batch operations
- **Item Suggestion Performance**: suggestItems pre-builds first-character index, limited to 100 suggestions

### Cleanup
- Removed unused LocalizationManager.getMinecraftLanguage() reflection method

## [2.0.3] - 2025-03-14

### Added
- **NBT Tag Recognition**: Selection tool uses NBT tags instead of names, supporting any language

### Fixed
- Selection tool not working after language switch
- Removed hardcoded Chinese/English strings from SelectionTool

### Improved
- All code comments unified to English
- All log output unified to English

## [2.0.2] - 2025-03-13

### Added
- **Smart Message System**: Auto-detects if client has the mod, dynamically selects translation method
- **Selection Tool Module**: Refactored into independent class
- **Thread-Safe Cache**: SmartCache class for concurrent access

### Fixed
- **Thread Safety**: All static collections use ConcurrentHashMap/CopyOnWriteArrayList
- **Memory Leaks**: Added ServerStoppingEvent cleanup
- **Config Paths**: Use FMLPaths API for correct file storage
- **Event Bus**: Fixed double registration, unified to annotation-based approach
- **Mod Icon**: Added logoFile config
- **Translation Formatting**: Fixed {0} placeholder not replacing, unified to %s format
- **Missing Translation Keys**: Added command.areamonitor.area.list.header and others
- **Parameter Order**: Fixed blacklist.item_removed Chinese translation parameter order
- **Command Colors**: Unified help command colors — gold (§6) commands + gray (§7) descriptions
- **Coordinate Display**: Fixed incorrect coordinates in area list and creation
- **Startup Crash**: Fixed ItemBlacklistManager static init causing "Registry is already frozen"

### Improved
- Constants replacing magic numbers
- Debug logging to avoid silent failures
- All comments and logs in English
- Code audit per Minecraft Forge 1.20.1 official dev specs

## [2.0.0] - 2024-03-06

### Added
- Full Chinese/English localization support
- Dynamic language switch command `/areamonitor language en/zh`
- Persistent language setting

### Fixed
- Tool name not updating on language switch
- Fallback handling for missing translation keys

### Improved
- Removed redundant code and unused imports
- Improved area check algorithm performance

## [1.0.3] - 2024-02-15

### Added
- Debug mode logging
- `/areamonitor performance` command

### Fixed
- Area boundary detection edge cases
- Player disconnect handling

## [1.0.2] - 2024-01-30

### Added
- Spatial partition system
- Area boundary cache

### Fixed
- Multiplayer thread safety
- Config file loading issues

## [1.0.1] - 2024-01-15

### Added
- Player whitelist system
- Item blacklist feature

### Fixed
- Command registration issues
- Permission checks

## [1.0.0] - 2024-01-01

### Added
- Initial release
- Multi-area monitoring with automatic game mode switching
- Particle effect visualization
- Full command system
- Multi-dimension support

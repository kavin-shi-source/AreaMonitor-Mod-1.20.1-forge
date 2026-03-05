# Changelog - AreaMonitor Mod

[![中文版本](https://img.shields.io/badge/中文版本-点击这里-red.svg)](CHANGELOG.md)

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2024-03-06

### Added
- **Complete Chinese Localization**: Full translation of all user interface elements, commands, and messages
- **Language Switch System**: Dynamic language switching with `/areamonitor language en/zh` commands
- **Enhanced Selection Tool**: Multi-language support for area selection tool names and feedback
- **New Translation Keys**: Added missing translation keys for all user-facing text
- **Language Configuration**: Persistent language settings that survive game restarts

### Fixed
- **Language Switch Issues**: Fixed tool names not updating when switching languages
- **String Matching Problems**: Resolved issues with selection tool recognition across languages
- **Component Caching**: Removed unnecessary component caching that caused performance issues
- **Translation Fallback**: Improved fallback handling for missing translation keys

### Optimized
- **Code Cleanup**: Removed redundant and unused code throughout the codebase
- **Import Optimization**: Cleaned up unused import statements
- **Method Refactoring**: Simplified complex methods and removed duplicate functionality
- **Memory Usage**: Reduced memory footprint by removing unnecessary caching systems
- **Performance**: Improved area checking algorithms and reduced server tick impact

### Changed
- **Tool Name System**: Changed from hardcoded English names to dynamic localized names
- **String Comparison**: Updated from `contains()` to more reliable comparison methods
- **Configuration Structure**: Streamlined configuration file organization
- **Error Handling**: Improved error messages and logging throughout

### Removed
- **Unused Imports**: Removed `ChatFormatting`, `ClientboundSetSubtitleTextPacket` imports
- **Redundant Methods**: Removed unused `getCurrentAreas()` and `getSpatialPartitionStats()` methods
- **Duplicate Trigger Classes**: Removed duplicate trigger implementations in AreaManager
- **Component Cache**: Removed unnecessary Guava-based component caching system
- **Unused Variables**: Cleaned up unused variables and parameters

## [1.0.3] - 2024-02-15

### Fixed
- **Area Detection**: Fixed edge cases in area boundary detection
- **Game Mode Switching**: Resolved timing issues with delayed mode changes
- **Player State Management**: Improved handling of player disconnections

### Added
- **Debug Mode**: Added detailed debug logging for troubleshooting
- **Performance Monitoring**: Added `/areamonitor performance` command

## [1.0.2] - 2024-01-30

### Fixed
- **Memory Leaks**: Fixed player data not being cleaned up properly
- **Concurrent Access**: Resolved thread safety issues in multi-player environments
- **Configuration Loading**: Fixed issues with config file loading on server start

### Added
- **Spatial Partitioning**: Implemented efficient area lookup system
- **Caching System**: Added intelligent caching for area boundaries

## [1.0.1] - 2024-01-15

### Fixed
- **Command Registration**: Fixed commands not registering properly
- **Permission Checks**: Improved permission system for commands
- **Area Creation**: Fixed issues with area creation from selections

### Added
- **Whitelist System**: Added player whitelist functionality
- **Item Blacklist**: Added restricted item system for areas

## [1.0.0] - 2024-01-01

### Added
- **Initial Release**: First public release of AreaMonitor mod
- **Basic Area Monitoring**: Core functionality for area-based game mode switching
- **Visual Selection**: Area selection tool with particle effects
- **Command System**: Complete command structure for area management
- **Configuration System**: JSON-based configuration for areas and settings
- **Multi-dimensional Support**: Support for all Minecraft dimensions
- **Performance Optimization**: Efficient algorithms for area detection

### Features
- Create and manage multiple monitoring areas
- Automatic game mode switching on area enter/exit
- Visual area boundaries with particle effects
- Player whitelist system
- Item blacklist functionality
- Real-time area visualization
- Comprehensive command system
- Configurable performance settings
- Multi-language support framework

---

## Versioning Scheme

This project uses Semantic Versioning:
- **MAJOR** version for incompatible API changes
- **MINOR** version for added functionality in a backwards compatible manner
- **PATCH** version for backwards compatible bug fixes

## Migration Notes

### From 1.x to 2.0
- Language settings are now persistent and stored in configuration
- Tool names are now localized and will update when switching languages
- Some configuration keys may have changed - backup your configs before updating
- Performance improvements may require server restart to take full effect
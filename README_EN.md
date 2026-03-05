# AreaMonitor Mod 1.20.1

[![中文版本](https://img.shields.io/badge/中文版本-点击这里-red.svg)](README.md)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)
![Forge](https://img.shields.io/badge/Forge-Compatible-blue.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

A Minecraft Forge mod for monitoring specific areas and automatically switching player game modes.

## Features

- **Multi-area monitoring**: Create and manage multiple independent monitoring areas
- **Game mode switching**: Automatically switch game modes when entering/leaving areas
- **Visual editor**: Particle effects for area boundaries, selection tool for area creation
- **Whitelist system**: Configurable player whitelist to bypass monitoring
- **Item blacklist**: Restrict teleport items and commands in specific areas
- **Multi-dimensional support**: Works across Overworld, Nether, and End dimensions
- **Performance optimized**: Efficient spatial partitioning and caching algorithms
- **Multi-language support**: Full Chinese and English localization
- **Advanced triggers**: Item hold triggers, player count triggers, periodic triggers
- **Real-time visualization**: Live particle effects showing area boundaries

## Quick Start

### Installation
1. Install Minecraft 1.20.1 with Forge
2. Place the mod JAR file in your `mods` folder
3. Launch Minecraft and join your server

### Creating Your First Area
1. **Get selection tool**: `/areamonitor visual tool`
2. **Select corners**: Right-click two opposite corner blocks with the wooden axe
3. **Create area**: `/areamonitor selection create my_area`
4. **Set enter mode**: `/areamonitor area setEnterMode my_area creative`
5. **Set leave mode**: `/areamonitor area setLeaveMode my_area survival`

### Language Switch
- Switch to English: `/areamonitor language en`
- Switch to Chinese: `/areamonitor language zh`

## Commands

### Basic Commands
- `/areamonitor status` - Show monitoring status
- `/areamonitor help` - Display all available commands
- `/areamonitor reload` - Reload configuration files
- `/areamonitor save` - Save current configuration

### Area Management
- `/areamonitor area info <name>` - Show detailed area information
- `/areamonitor area list` - List all configured areas
- `/areamonitor area delete <name>` - Delete an area

### Visual Tools
- `/areamonitor visual tool` - Get the area selection tool
- `/areamonitor visual show <area>` - Show area boundaries
- `/areamonitor visual hide` - Hide area boundaries

### Selection Tools
- `/areamonitor selection create <name>` - Create area from current selection
- `/areamonitor selection cancel` - Cancel current selection
- `/areamonitor selection info` - Show current selection details

### Whitelist Management
- `/areamonitor whitelist add <player>` - Add player to whitelist
- `/areamonitor whitelist remove <player>` - Remove player from whitelist
- `/areamonitor whitelist list` - List whitelisted players
- `/areamonitor whitelist clear` - Clear all whitelisted players

### Configuration
- `/areamonitor config reload` - Reload all configuration files
- `/areamonitor config generate` - Generate missing configuration files

## Configuration Files

The mod uses JSON configuration files located in `config/areamonitor/`:

### areas.json
Contains all area definitions with their coordinates, dimensions, and game modes.

### blacklist.json
Defines restricted items and commands for each area.

## Supported Game Modes

- `survival` - Survival mode
- `creative` - Creative mode
- `adventure` - Adventure mode
- `spectator` - Spectator mode

## Performance Features

- **Spatial Partitioning**: Efficient area lookup using grid-based spatial partitioning
- **Caching**: Intelligent caching of area boundaries and player positions
- **Optimized Checks**: Reduced frequency area checks to minimize server impact
- **Memory Management**: Automatic cleanup of disconnected player data

## Security Features

- **Input Validation**: All commands validate input parameters
- **Area Size Limits**: Prevents excessively large areas (max 1000x1000 blocks)
- **Permission System**: Commands require appropriate server permissions
- **Safe Mode Switching**: Delayed game mode changes with player state verification

## Contributing

Contributions are welcome! Please feel free to:
- Submit Issues for bug reports or feature requests
- Create Pull Requests for improvements
- Help with translations and documentation

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

If you encounter any issues:
- Create an Issue on GitHub
- Check the project documentation
- Review the configuration examples
# AreaMonitor Mod 1.20.1

[![中文版本](https://img.shields.io/badge/中文版本-点击这里-red.svg)](README.md)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-brightgreen.svg)](https://modrinth.com/mod/areamonitor)
[![GitHub](https://img.shields.io/badge/GitHub-Repository-black.svg)](https://github.com/kavin-shi-source/AreaMonitor-Mod-1.20.1-forge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)
![Forge](https://img.shields.io/badge/Forge-Compatible-blue.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

A Minecraft Forge mod for monitoring specific areas and automatically switching player game modes.

## Features

- **Multi-area monitoring** - Create and manage multiple independent monitoring areas
- **Game mode switching** - Automatically switch game modes when entering/leaving areas
- **Visual editor** - Particle effects for area boundaries, selection tool for area creation
- **Whitelist system** - Configurable player whitelist to bypass monitoring
- **Item blacklist** - Restrict teleport items and commands in specific areas
- **Multi-dimensional support** - Works across Overworld, Nether, and End
- **Multi-language support** - Full Chinese and English localization

## Quick Start

👉 **For detailed tutorial, see [Quick Start Guide](QUICK_START_EN.md)**

### Simple Installation
1. Install Minecraft 1.20.1 with Forge
2. Place the mod JAR file in your `mods` folder
3. Start the server

### Create Your First Area
```
/areamonitor visual tool                    # Get selection tool
/areamonitor selection create my_area       # Create area
/areamonitor area setEnterMode my_area creative
/areamonitor area setLeaveMode my_area survival
```

## Configuration Files

Configuration files are located in `config/areamonitor/`:

| File | Description |
|------|-------------|
| `areas.json` | Area definitions, coordinates, dimensions, and game mode settings |
| `blacklist.json` | Restricted items and commands for each area |

## Supported Game Modes

| Mode | Description |
|------|-------------|
| `survival` | Survival mode |
| `creative` | Creative mode |
| `adventure` | Adventure mode |
| `spectator` | Spectator mode |

## Links

- 📥 [Download on Modrinth](https://modrinth.com/mod/areamonitor)
- 📖 [Changelog](CHANGELOG_EN.md)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.txt) file for details.

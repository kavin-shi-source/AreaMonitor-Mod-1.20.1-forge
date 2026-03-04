# AreaMonitor Mod

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)
![Forge](https://img.shields.io/badge/Forge-Compatible-blue.svg)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red.svg)

A powerful Minecraft server management mod for monitoring specific areas and automatically switching player game modes.

## ✨ Features

- **Area Monitoring**: Monitor player activity within specific coordinate areas
- **Game Mode Switching**: Automatically switch game modes when entering/leaving areas
- **Whitelist System**: Configurable player exemption list
- **Multi-Dimension Support**: Supports Overworld, Nether, End, and custom dimensions
- **Highly Configurable**: All parameters adjustable via configuration file
- **Command System**: Complete server management command set
- **Performance Optimized**: Efficient area checking with caching mechanisms
- **Safety Features**: Input validation and area size limits

## 🚀 Quick Start

### Requirements
- Minecraft 1.20.1
- Forge Mod Loader 47.0.35+
- Java 17+

### Installation
1. Download the latest version of the mod file
2. Place the file in your Minecraft `mods` folder
3. Start the game and configure monitoring areas

## 📋 Commands

### Basic Commands
- `/areamonitor toggle` - Toggle monitoring status
- `/areamonitor status` - View mod status
- `/areamonitor help` - Show help information

### Area Settings
- `/areamonitor setArea <minX> <minZ> <maxX> <maxZ>` - Set monitoring area
- `/areamonitor setDimension <dimension>` - Set target dimension

### Mode Settings
- `/areamonitor setEnterMode <mode>` - Set game mode when entering area
- `/areamonitor setLeaveMode <mode>` - Set game mode when leaving area

### Whitelist Management
- `/areamonitor whitelist add <player>` - Add to whitelist
- `/areamonitor whitelist remove <player>` - Remove from whitelist
- `/areamonitor whitelist list` - View whitelist
- `/areamonitor whitelist clear` - Clear whitelist

## ⚙️ Configuration

Mod configuration file is located at `config/area-monitor-common.toml` with the following configurable options:

- **Enable/Disable monitoring functionality**
- **Target dimension settings**
- **Monitoring area coordinate ranges**
- **Enter/leave area game modes**
- **Message display settings**
- **Performance optimization settings**

## 🎮 Usage Examples

### Create a Creative Area
- Set monitoring area (X:-100 to 100, Z:-100 to 100)
  ```
  /areamonitor setArea -100 -100 100 100
  ```
- Set target dimension to Overworld
  ```
  /areamonitor setDimension minecraft:overworld
  ```
- Set enter mode to creative, leave mode to survival
  ```
  /areamonitor setEnterMode creative
  /areamonitor setLeaveMode survival
  ```
- Add administrators to whitelist
  ```
  /areamonitor whitelist add AdminName
  ```

### Server Management
- Check current status
  ```
  /areamonitor status
  ```
- Temporarily disable monitoring
  ```
  /areamonitor toggle
  ```

## 🔧 Development

- **Author**: kavinshi
- **Version**: 1.02
- **GitHub**: [kavin-shi-source/AreaMonitor-Mod](https://github.com/kavin-shi-source/AreaMonitor-Mod)
- **License**: All Rights Reserved

### Building from Source
```bash
./gradlew build
```

The compiled JAR will be in `build/libs/`.

## 🛡️ Security Features

- **Input Validation**: All commands validate input parameters
- **Area Size Limits**: Prevents excessively large monitoring areas (max 1000x1000)
- **Permission System**: Commands require appropriate server permissions
- **Safe Mode Switching**: Delayed game mode changes with player state verification
- **Error Handling**: Comprehensive exception handling and logging

## 📈 Performance Optimizations

- **Caching System**: Boundary value caching for efficient area checking
- **Tick Optimization**: Configurable check intervals (default: 5 ticks)
- **Memory Management**: Automatic cleanup of disconnected player data
- **Concurrent Safety**: Thread-safe data structures for multi-player environments

## 🤝 Contributing

Contributions are welcome! Please feel free to:
- Submit Issues for bug reports or feature requests
- Create Pull Requests for improvements
- Help with documentation and translations

## 📞 Support

If you encounter any issues:
- Create an Issue on GitHub
- Check the project documentation
- Review the configuration examples

---

*Thank you for using AreaMonitor Mod!*

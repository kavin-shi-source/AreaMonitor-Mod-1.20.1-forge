# 区域监控模组 (AreaMonitor Mod) / Area Monitor Mod

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)
![Forge](https://img.shields.io/badge/Forge-Compatible-blue.svg)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red.svg)

一个功能强大的Minecraft服务器管理模组，用于监控特定区域并自动切换玩家游戏模式。
A powerful Minecraft server management mod for monitoring specific areas and automatically switching player game modes.

---

## ✨ 功能特性 / Features

### 中文
- **区域监控**：监控特定坐标区域内的玩家活动
- **游戏模式切换**：进入/离开区域时自动切换游戏模式
- **白名单系统**：可配置免监控玩家列表
- **多维度支持**：支持主世界、下界、末地等多个维度
- **高度可配置**：所有参数均可通过配置文件调整
- **命令系统**：完整的服务器管理命令集
- **性能优化**：高效的区域检查算法和缓存机制
- **安全特性**：输入验证和区域大小限制

### English
- **Area Monitoring**: Monitor player activity within specific coordinate areas
- **Game Mode Switching**: Automatically switch game modes when entering/leaving areas
- **Whitelist System**: Configurable player exemption list
- **Multi-Dimension Support**: Supports Overworld, Nether, End, and custom dimensions
- **Highly Configurable**: All parameters adjustable via configuration file
- **Command System**: Complete server management command set
- **Performance Optimized**: Efficient area checking with caching mechanisms
- **Security Features**: Input validation and area size limits

## ✨ 功能特性

- **区域监控**：监控特定坐标区域内的玩家活动
- **游戏模式切换**：进入/离开区域时自动切换游戏模式
- **白名单系统**：可配置免监控玩家列表
- **多维度支持**：支持主世界、下界、末地等多个维度
- **高度可配置**：所有参数均可通过配置文件调整
- **命令系统**：完整的服务器管理命令集

## 🚀 快速开始 / Quick Start

### 安装要求 / Requirements
- Minecraft 1.20.1
- Forge Mod Loader 47.0.35+
- Java 17+

### 安装步骤 / Installation
1. 下载最新版本的模组文件 / Download the latest version of the mod file
2. 将文件放入 Minecraft 的 `mods` 文件夹 / Place the file in your Minecraft `mods` folder
3. 启动游戏并配置监控区域 / Start the game and configure monitoring areas

## 📋 命令列表 / Commands

### 基本命令 / Basic Commands
- `/areamonitor toggle` - 切换监控状态 / Toggle monitoring status
- `/areamonitor status` - 查看模组状态 / View mod status
- `/areamonitor help` - 显示帮助信息 / Show help information

### 区域设置 / Area Settings
- `/areamonitor setArea <minX> <minZ> <maxX> <maxZ>` - 设置监控区域 / Set monitoring area
- `/areamonitor setDimension <dimension>` - 设置目标维度 / Set target dimension

### 模式设置 / Mode Settings
- `/areamonitor setEnterMode <mode>` - 设置进入区域时的游戏模式 / Set game mode when entering area
- `/areamonitor setLeaveMode <mode>` - 设置离开区域时的游戏模式 / Set game mode when leaving area

### 白名单管理 / Whitelist Management
- `/areamonitor whitelist add <player>` - 添加白名单 / Add to whitelist
- `/areamonitor whitelist remove <player>` - 移除白名单 / Remove from whitelist
- `/areamonitor whitelist list` - 查看白名单 / View whitelist
- `/areamonitor whitelist clear` - 清空白名单 / Clear whitelist

## ⚙️ 配置说明 / Configuration

模组配置文件位于 `config/area-monitor-common.toml`，包含以下可配置项：
The mod configuration file is located at `config/area-monitor-common.toml` with the following configurable options:

- **启用/禁用监控功能** / **Enable/Disable monitoring functionality**
- **目标维度设置** / **Target dimension settings**
- **监控区域坐标范围** / **Monitoring area coordinate ranges**
- **进入/离开区域游戏模式** / **Enter/leave area game modes**
- **消息显示设置** / **Message display settings**
- **性能优化设置** / **Performance optimization settings**

## 🎮 使用示例 / Usage Examples

### 创建活动区域 / Create a Creative Area
- 设置监控区域（X:-100到100, Z:-100到100）/ Set monitoring area (X:-100 to 100, Z:-100 to 100)
  ```
  /areamonitor setArea -100 -100 100 100
  ```
- 设置目标维度为主世界 / Set target dimension to Overworld
  ```
  /areamonitor setDimension minecraft:overworld
  ```
- 设置进入区域为冒险模式，离开为生存模式 / Set enter mode to creative, leave mode to survival
  ```
  /areamonitor setEnterMode creative
  /areamonitor setLeaveMode survival
  ```
- 添加管理员到白名单 / Add administrators to whitelist
  ```
  /areamonitor whitelist add AdminName
  ```

## 🔧 开发信息 / Development

- **作者/ Author**: kavinshi
- **版本/ Version**: 1.02
- **GitHub**: [kavin-shi-source/AreaMonitor-Mod](https://github.com/kavin-shi-source/AreaMonitor-Mod)
- **许可证/ License**: All Rights Reserved

### 构建说明 / Building from Source
```bash
./gradlew build
```

编译后的JAR文件将位于 `build/libs/` 目录。
The compiled JAR will be in `build/libs/`.

## 🛡️ 安全特性 / Security Features

- **输入验证/Input Validation**: 所有命令验证输入参数 / All commands validate input parameters
- **区域大小限制/Area Size Limits**: 防止过大的监控区域（最大1000x1000）/ Prevents excessively large monitoring areas (max 1000x1000)
- **权限系统/Permission System**: 命令需要适当的服务器权限 / Commands require appropriate server permissions
- **安全模式切换/Safe Mode Switching**: 延迟游戏模式更改并验证玩家状态 / Delayed game mode changes with player state verification
- **错误处理/Error Handling**: 全面的异常处理和日志记录 / Comprehensive exception handling and logging

## 📈 性能优化 / Performance Optimizations

- **缓存系统/Caching System**: 边界值缓存以提高区域检查效率 / Boundary value caching for efficient area checking
- **Tick优化/Tick Optimization**: 可配置检查间隔（默认：5 ticks）/ Configurable check intervals (default: 5 ticks)
- **内存管理/Memory Management**: 自动清理断开连接的玩家数据 / Automatic cleanup of disconnected player data
- **并发安全/Concurrent Safety**: 多玩家环境的线程安全数据结构 / Thread-safe data structures for multi-player environments

## 🤝 贡献 / Contributing

欢迎提交 Issue 和 Pull Request 来改进这个模组！
Contributions are welcome! Please feel free to:
- Submit Issues for bug reports or feature requests
- Create Pull Requests for improvements
- Help with documentation and translations

## 📞 支持 / Support

如果您遇到任何问题，请通过以下方式联系：
If you encounter any issues:
- 在 GitHub 上创建 Issue / Create an Issue on GitHub
- 查看项目文档 / Check the project documentation
- 查看配置示例 / Review the configuration examples

---

*感谢使用区域监控模组！* / *Thank you for using AreaMonitor Mod!*
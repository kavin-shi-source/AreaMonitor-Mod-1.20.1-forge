# 区域监控模组 (AreaMonitor Mod)

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)
![Forge](https://img.shields.io/badge/Forge-兼容-blue.svg)

一个Minecraft 服务器管理模组，用于监控特定区域并自动切换玩家游戏模式。

## ✨ 功能特性

- **区域监控**：监控特定坐标区域内的玩家活动
- **游戏模式切换**：进入/离开区域时自动切换游戏模式
- **白名单系统**：可配置免监控玩家列表
- **多维度支持**：支持主世界、下界、末地等多个维度
- **高度可配置**：所有参数均可通过配置文件调整
- **命令系统**：完整的服务器管理命令集

## 🚀 快速开始

### 安装要求
- Minecraft 1.20.1
- Forge Mod Loader

### 安装步骤
1. 下载最新版本的模组文件
2. 将文件放入 Minecraft 的 `mods` 文件夹
3. 启动游戏并配置监控区域

## 📋 命令列表

### 基本命令
- `/areamonitor toggle` - 切换监控状态
- `/areamonitor status` - 查看模组状态
- `/areamonitor help` - 显示帮助信息

### 区域设置
- `/areamonitor setArea <minX> <minZ> <maxX> <maxZ>` - 设置监控区域
- `/areamonitor setDimension <维度>` - 设置目标维度

### 模式设置
- `/areamonitor setEnterMode <模式>` - 设置进入区域时的游戏模式
- `/areamonitor setLeaveMode <模式>` - 设置离开区域时的游戏模式

### 白名单管理
- `/areamonitor whitelist add <玩家名>` - 添加白名单
- `/areamonitor whitelist remove <玩家名>` - 移除白名单
- `/areamonitor whitelist list` - 查看白名单
- `/areamonitor whitelist clear` - 清空白名单

## ⚙️ 配置说明

模组配置文件位于 `config/area-monitor-common.toml`，包含以下可配置项：

- **启用/禁用监控功能**
- **目标维度设置**
- **监控区域坐标范围**
- **进入/离开区域游戏模式**
- **消息显示设置**

## 🎮 使用示例
### 创建活动区域
- 设置监控区域（X:-100到100, Z:-100到100）
- /areamonitor setArea -100 -100 100 100
- 设置目标维度为主世界
- /areamonitor setDimension minecraft:overworld
- 设置进入区域为冒险模式，离开为生存模式
- /areamonitor setEnterMode adventure
- /areamonitor setLeaveMode survival
- 添加管理员到白名单
- /areamonitor whitelist add PlayerName

## 🔧 开发信息

- **作者**: kavinshi
- **版本**: 1.02
- **GitHub**: [kavin-shi-source/AreaMonitor-Mod](https://github.com/kavin-shi-source/AreaMonitor-Mod)
- **许可证**: 详见 LICENSE.txt

## 🤝 贡献

欢迎提交 Issue 和 Pull Request 来改进这个模组！

## 📞 支持

如果您遇到任何问题，请通过以下方式联系：
- 在 GitHub 上创建 Issue
- 查看项目文档

---

*感谢使用区域监控模组！*
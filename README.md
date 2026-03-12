# AreaMonitor Mod 1.20.1

[![English Version](https://img.shields.io/badge/English-Version-blue.svg)](README_EN.md)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-brightgreen.svg)](https://modrinth.com/mod/areamonitor)
[![GitHub](https://img.shields.io/badge/GitHub-Repository-black.svg)](https://github.com/kavin-shi-source/AreaMonitor-Mod-1.20.1-forge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)
![Forge](https://img.shields.io/badge/Forge-Compatible-blue.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

一个用于监控特定区域并自动切换玩家游戏模式的 Minecraft Forge 模组。

## 功能特性

- **多区域监控** - 创建和管理多个独立的监控区域
- **游戏模式切换** - 进入/离开区域时自动切换游戏模式
- **可视化编辑器** - 粒子效果显示区域边界，选择工具创建区域
- **白名单系统** - 可配置免监控玩家列表
- **物品黑名单** - 限制区域内使用传送道具和命令
- **多维度支持** - 支持主世界、下界、末地
- **多语言支持** - 完整的中文和英文本地化

## 快速开始

👉 **详细教程请查看 [快速入门指南](QUICK_START.md)**

### 简易安装
1. 安装 Minecraft 1.20.1 和 Forge
2. 将模组 JAR 文件放入 `mods` 文件夹
3. 启动服务器

### 创建第一个区域
```
/areamonitor visual tool                    # 获取选择工具
/areamonitor selection create 我的区域       # 创建区域
/areamonitor area setEnterMode 我的区域 creative
/areamonitor area setLeaveMode 我的区域 survival
```

## 配置文件

配置文件位于 `config/areamonitor/`：

| 文件 | 说明 |
|------|------|
| `areas.json` | 区域定义、坐标、维度和游戏模式设置 |
| `blacklist.json` | 每个区域的限制物品和命令 |

## 支持的游戏模式

| 模式 | 说明 |
|------|------|
| `survival` | 生存模式 |
| `creative` | 创造模式 |
| `adventure` | 冒险模式 |
| `spectator` | 旁观模式 |

## 链接

- 📥 [Modrinth 下载](https://modrinth.com/mod/areamonitor)
- 📖 [更新日志](CHANGELOG.md)

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE.txt) 文件。
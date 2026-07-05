# AreaMonitor Mod 1.20.1

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.4.0-blue.svg)](https://files.minecraftforge.net)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE.txt)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-brightgreen.svg)](https://modrinth.com/mod/areamonitor)
[![CurseForge](https://img.shields.io/badge/CurseForge-Download-orange.svg)](https://www.curseforge.com/minecraft/mc-mods/areamonitor)

[English Version](README_EN.md)

一个功能丰富的 Minecraft Forge 服务器端模组，用于**监控指定区域**并**自动切换玩家游戏模式**。支持矩形/圆形/多边形区域、九项保护规则、可编程触发器、GUI 可视化管理等。

---

## 功能概览

| 模块 | 功能 |
|------|------|
| **区域管理** | 创建/删除/列表/启用切换/信息查看/导出导入 |
| **游戏模式切换** | 进入/离开区域时自动切换游戏模式 |
| **区域边界** | 矩形、圆形、多边形（3-32 个顶点）三种边界类型 |
| **保护系统** | 9 种保护类型：方块破坏/放置/交互、PVP、爆炸、实体伤害、容器交互、流体放置、物品丢弃 |
| **保护白名单** | 每区域独立的保护例外名单 |
| **触发器系统** | 进入/离开独立触发器：命令执行、音效、Title、ActionBar、药水效果、传送、冷却时间、防抖 |
| **触发条件** | 物品持有/时间段/天气/最少玩家数 AND 逻辑组合 |
| **物品黑名单** | 禁止区域内使用指定物品，支持智能补全 |
| **白名单系统** | 全局白名单 + 区域白名单，可跳过游戏模式切换 |
| **时间调度** | 按游戏内时间段自动启用/禁用区域（支持跨午夜） |
| **条件激活** | 按在线玩家数或特定玩家存在判断是否激活 |
| **区域链** | 进入 A 区域 → 自动传送到 B 区域 → C 区域…… |
| **选区工具** | 手持木斧右键选择 + 多边形顶点采集模式 |
| **边界可视化** | 粒子效果实时显示区域轮廓 |
| **GUI 管理界面** | Glass Morphism 浮动窗口，完整可视化管理 | 
| **配置文件** | JSON 格式，支持热加载和自动生成 |
| **备份恢复** | 一键备份配置文件夹（时间戳命名） |
| **统计追踪** | 每个区域的进入次数、最后访客、访问时间 |
| **性能监控** | `/areamonitor performance` 查看运行时指标 |
| **空间分区** | 基于网格的空间索引，O(k) 高效查找 |
| **审计日志** | 区域创建/删除/切换操作自动记录 |
| **多语言** | 简体中文 / English 完整本地化 |

---

## 快速开始

### 安装

1. 安装 Minecraft 1.20.1 + Forge 47.4.0+
2. 将 `areamonitor-2.0.4.jar` 放入服务器 `mods/` 文件夹
3. 启动服务器，配置文件自动生成于 `config/areamonitor/`

### 创建第一个区域

```mcfunction
# 获取选区工具（木斧）
/areamonitor visual tool

# 右键选择两个对角点后创建
/areamonitor selection create 主城

# 设置游戏模式
/areamonitor area setEnterMode 主城 creative
/areamonitor area setLeaveMode 主城 survival
```

---

## 命令速览

所有命令根为 `/areamonitor`，需要 OP 权限（等级 2）。共 **14 个子命令组，60+ 条指令**。

### 区域 (`area`)
```
create <name>  delete <name>  list  toggle <name>  info <name>
setEnterMode <area> <mode>  setLeaveMode <area> <mode>
export <name>  import <name> <json>
```

### 保护 (`protect`)
```
<area> all on|off          — 全保护开关
<area> <type> on|off       — 单项保护开关
<area> info                — 查看保护状态
```

保护类型：`blockBreak` `blockPlace` `blockInteract` `pvp` `explosion` `entityDamage` `containerInteract` `fluidPlace` `itemDrop`

### 触发器 (`trigger`)
```
<area> enter|leave cmd add|remove|list|clear
<area> enter|leave sound <soundId>|clear
<area> enter|leave title <main> [<sub>]|clear
<area> enter|leave tp <dim> <x> <y> <z>|clear
<area> enter|leave info
```

### 黑名单 (`blacklist`)
```
info  reload
area <area> add <item>|remove <item>|list|toggle
```

### 选区 (`selection`)
```
create <name>  cancel  info  tutorial
polygon start|finish
```

### 可视化 (`visual`)
```
tool  show <area>  hide
```

### 白名单 (`whitelist`)
```
add <player>  remove <player>  list  clear
```

### 其他命令
```
/areamonitor toggle         — 全局监控开关
/areamonitor config reload  — 热加载配置
/areamonitor config generate— 生成默认配置
/areamonitor stats          — 区域统计
/areamonitor backup         — 配置备份
/areamonitor performance    — 性能指标
/areamonitor gui            — 打开 GUI 管理界面
/areamonitor help           — 命令帮助
```

---

## 配置文件

配置文件位于 `config/areamonitor/`：

| 文件 | 说明 |
|------|------|
| `areas.json` | 所有区域定义（坐标、维度、游戏模式、保护、触发器、调度、条件、区域链） |
| `blacklist.json` | 物品与命令黑名单 |
| `backups/` | 备份目录（`/areamonitor backup` 自动生成时间戳子文件夹） |

---

## 游戏模式

| 模式 | 命令参数 |
|------|----------|
| 生存 | `survival` |
| 创造 | `creative` |
| 冒险 | `adventure` |
| 旁观 | `spectator` |

---

## 触发器条件

触发器支持 AND 组合的激活条件（进入和离开各自独立配置）：

| 条件 | 说明 |
|------|------|
| `playerHasItem` | 玩家背包含指定物品 |
| `timeMin` / `timeMax` | 游戏时间内（0-24000 ticks） |
| `weather` | 天气：`clear` / `rain` / `thunder` |
| `minPlayers` | 服务器在线人数 ≥ N |
| `requirePlayer` | 指定玩家必须在线 |

触发器动作按顺序执行：命令 → 音效 → Title → ActionBar → 药水 → 传送，每项均可有冷却时间和防抖。

---

## 链接

- 📥 [Modrinth 下载](https://modrinth.com/mod/areamonitor)
- 📥 [CurseForge 下载](https://www.curseforge.com/minecraft/mc-mods/areamonitor)
- 📖 [更新日志](CHANGELOG.md)
- 🐛 [问题反馈](https://github.com/kavin-shi-source/AreaMonitor-Mod-1.20.1-forge/issues)

## 许可证

本项目采用 GPL-3.0 许可证 — 详见 [LICENSE](LICENSE.txt)。

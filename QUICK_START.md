# AreaMonitor 模组快速入门指南

[![English Version](https://img.shields.io/badge/English-Version-blue.svg)](QUICK_START_EN.md)

## 目录
- [安装](#安装)
- [创建第一个区域](#创建第一个区域)
- [命令参考](#命令参考)
- [常见使用案例](#常见使用案例)
- [故障排除](#故障排除)

## 安装

### 前提条件
- Minecraft 1.20.1
- Forge 已安装
- 服务器管理员 (OP) 权限

### 安装步骤
1. 下载 AreaMonitor 模组 JAR 文件
2. 放入 `mods` 文件夹
3. 启动服务器

## 创建第一个区域

### 第1步：获取选择工具
```
/areamonitor visual tool
```

### 第2步：选择区域
1. 手持木斧，右键点击第一个角落
2. 右键点击对角角落
3. 查看区域信息确认

### 第3步：创建区域
```
/areamonitor selection create 我的创造区
```

### 第4步：配置游戏模式
```
/areamonitor area setEnterMode 我的创造区 creative
/areamonitor area setLeaveMode 我的创造区 survival
```

### 第5步：测试
走进区域 → 自动切换创造模式
走出区域 → 自动切换生存模式

## 命令参考

### 基础命令
| 命令 | 说明 |
|------|------|
| `/areamonitor status` | 显示监控状态 |
| `/areamonitor help` | 显示所有命令 |
| `/areamonitor toggle` | 开关监控 |
| `/areamonitor reload` | 重载配置 |
| `/areamonitor save` | 保存配置 |

### 区域管理
| 命令 | 说明 |
|------|------|
| `/areamonitor area list` | 列出所有区域 |
| `/areamonitor area info <名称>` | 显示区域详情 |
| `/areamonitor area delete <名称>` | 删除区域 |
| `/areamonitor area toggle <名称>` | 开关区域 |
| `/areamonitor area setEnterMode <区域> <模式>` | 设置进入模式 |
| `/areamonitor area setLeaveMode <区域> <模式>` | 设置离开模式 |

### 可视化工具
| 命令 | 说明 |
|------|------|
| `/areamonitor visual tool` | 获取选择工具 |
| `/areamonitor visual show <区域>` | 显示区域边界 |
| `/areamonitor visual hide` | 隐藏区域边界 |

### 选择工具
| 命令 | 说明 |
|------|------|
| `/areamonitor selection create <名称>` | 从选择创建区域 |
| `/areamonitor selection cancel` | 取消选择 |
| `/areamonitor selection info` | 显示选择信息 |
| `/areamonitor selection tutorial` | 显示教程 |

### 白名单管理
| 命令 | 说明 |
|------|------|
| `/areamonitor whitelist add <玩家>` | 添加玩家 |
| `/areamonitor whitelist remove <玩家>` | 移除玩家 |
| `/areamonitor whitelist list` | 列出白名单 |
| `/areamonitor whitelist clear` | 清空白名单 |

### 黑名单管理
| 命令 | 说明 |
|------|------|
| `/areamonitor blacklist info` | 显示当前限制 |
| `/areamonitor blacklist area <区域> add <物品>` | 添加黑名单物品 |
| `/areamonitor blacklist area <区域> remove <物品>` | 移除黑名单物品 |
| `/areamonitor blacklist area <区域> list` | 列出黑名单 |
| `/areamonitor blacklist area <区域> toggle` | 开关黑名单 |

### 语言设置
| 命令 | 说明 |
|------|------|
| `/areamonitor language` | 显示当前语言 |
| `/areamonitor language en` | 切换英文 |
| `/areamonitor language zh` | 切换中文 |

## 常见使用案例

### 创造建筑区
```
/areamonitor selection create 创造区
/areamonitor area setEnterMode 创造区 creative
/areamonitor area setLeaveMode 创造区 survival
```

### PVP 竞技场
```
/areamonitor selection create pvp竞技场
/areamonitor area setEnterMode pvp竞技场 adventure
/areamonitor area setLeaveMode pvp竞技场 survival
```

### 保护区
```
/areamonitor selection create 出生点保护
/areamonitor area setEnterMode 出生点保护 adventure
/areamonitor area setLeaveMode 出生点保护 survival
```

## 故障排除

### 选择工具不工作
- 确保有 OP 权限
- 确保工具在主手中
- 重新获取工具：`/areamonitor visual tool`

### 区域不触发
- 检查区域大小（≤1000x1000）
- 确认在正确维度
- 检查是否在白名单中

### 语言问题
- 切换语言：`/areamonitor language zh`
- 检查 lang 文件夹

## 获取帮助

- 使用 `/areamonitor help` 查看命令
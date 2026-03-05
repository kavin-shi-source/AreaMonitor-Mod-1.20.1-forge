# AreaMonitor Mod 1.20.1

[![English Version](https://img.shields.io/badge/English-Version-blue.svg)](README_EN.md)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)
![Forge](https://img.shields.io/badge/Forge-Compatible-blue.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

一个用于监控特定区域并自动切换玩家游戏模式的 Minecraft Forge 模组。

## 功能特性

- **多区域监控**：创建和管理多个独立的监控区域
- **游戏模式切换**：进入/离开区域时自动切换游戏模式
- **可视化编辑器**：粒子效果显示区域边界，选择工具创建区域
- **白名单系统**：可配置免监控玩家列表
- **物品黑名单**：限制区域内使用传送道具和命令
- **多维度支持**：支持主世界、下界、末地等多个维度
- **性能优化**：高效的空间分区和缓存算法
- **多语言支持**：完整的中文和英文本地化
- **高级触发器**：物品持有触发、玩家数量触发、周期性触发
- **实时可视化**：实时粒子效果显示区域边界

## 快速开始

### 安装
1. 安装 Minecraft 1.20.1 和 Forge
2. 将模组 JAR 文件放入 `mods` 文件夹
3. 启动 Minecraft 并加入服务器

### 创建第一个区域
1. **获取选择工具**：`/areamonitor visual tool`
2. **选择角落**：用木斧右键点击两个对角的方块
3. **创建区域**：`/areamonitor selection create 我的区域`
4. **设置进入模式**：`/areamonitor area setEnterMode 我的区域 creative`
5. **设置离开模式**：`/areamonitor area setLeaveMode 我的区域 survival`

### 语言切换
- 切换到英文：`/areamonitor language en`
- 切换到中文：`/areamonitor language zh`

## 命令列表

### 基础命令
- `/areamonitor status` - 显示监控状态
- `/areamonitor help` - 显示所有可用命令
- `/areamonitor reload` - 重新加载配置文件
- `/areamonitor save` - 保存当前配置

### 区域管理
- `/areamonitor area info <名称>` - 显示详细区域信息
- `/areamonitor area list` - 列出所有配置的区域
- `/areamonitor area delete <名称>` - 删除区域

### 可视化工具
- `/areamonitor visual tool` - 获取区域选择工具
- `/areamonitor visual show <区域>` - 显示区域边界
- `/areamonitor visual hide` - 隐藏区域边界

### 选择工具
- `/areamonitor selection create <名称>` - 从当前选择创建区域
- `/areamonitor selection cancel` - 取消当前选择
- `/areamonitor selection info` - 显示当前选择详情

### 白名单管理
- `/areamonitor whitelist add <玩家>` - 添加玩家到白名单
- `/areamonitor whitelist remove <玩家>` - 从白名单移除玩家
- `/areamonitor whitelist list` - 列出白名单玩家
- `/areamonitor whitelist clear` - 清空所有白名单玩家

### 配置管理
- `/areamonitor config reload` - 重新加载所有配置文件
- `/areamonitor config generate` - 生成缺失的配置文件

## 配置文件

模组使用 JSON 配置文件，位于 `config/areamonitor/`：

### areas.json
包含所有区域的定义，包括坐标、维度和游戏模式设置。

### blacklist.json
定义每个区域的限制物品和命令。

## 支持的游戏模式

- `survival` - 生存模式
- `creative` - 创造模式
- `adventure` - 冒险模式
- `spectator` - 旁观模式

## 性能特性

- **空间分区**：使用基于网格的空间分区进行高效区域查找
- **缓存系统**：智能缓存区域边界和玩家位置
- **优化检查**：降低区域检查频率以最小化服务器影响
- **内存管理**：自动清理断开连接的玩家数据

## 安全特性

- **输入验证**：所有命令都验证输入参数
- **区域大小限制**：防止过大的区域（最大1000x1000方块）
- **权限系统**：命令需要适当的服务器权限
- **安全模式切换**：延迟游戏模式更改并验证玩家状态

## 贡献

欢迎贡献！请随时：
- 提交 Issue 报告错误或功能请求
- 创建 Pull Request 进行改进
- 帮助翻译和文档工作

## 许可证

本项目采用 MIT 许可证 - 详见 LICENSE 文件。

## 支持

如果遇到问题：
- 在 GitHub 上创建 Issue
- 查看项目文档
- 查看配置示例
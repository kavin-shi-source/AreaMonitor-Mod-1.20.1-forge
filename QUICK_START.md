# AreaMonitor 模组快速入门指南

[![English Version](https://img.shields.io/badge/English-Version-blue.svg)](QUICK_START_EN.md)

## 目录
- [安装](#安装)
- [创建你的第一个区域](#创建你的第一个区域)
- [高级用法](#高级用法)
- [故障排除](#故障排除)

## 安装

### 前提条件
- Minecraft 1.20.1
- Forge 已安装并可正常工作
- 服务器管理员 (OP) 权限

### 安装步骤
1. 下载 AreaMonitor 模组 JAR 文件
2. 将其放入 Minecraft 的 `mods` 文件夹
3. 启动 Minecraft 客户端或服务器
4. 检查日志中是否有 "AreaMonitor" 条目来验证模组已加载

## 创建你的第一个区域

### 第1步：获取选择工具
```
/areamonitor visual tool
```
这会给你一个木斧，用于选择你的区域。

### 第2步：选择第一个角落
1. 在主手中持有木斧
2. 右键点击你想要的区域的第一个角落方块
3. 你会看到绿色消息确认第一个点

### 第3步：选择第二个角落
1. 继续持有木斧
2. 右键点击对角的角落方块
3. 你会看到详细的区域信息，包括：
   - 两个点的坐标
   - 区域大小（方块数）
   - 当前维度

### 第4步：创建区域
```
/areamonitor selection create 我的创造区
```
将 `我的创造区` 替换为你想要的区域名称。

### 第5步：配置游戏模式
设置玩家进入区域时的游戏模式：
```
/areamonitor area setEnterMode 我的创造区 creative
```

设置玩家离开区域时的游戏模式：
```
/areamonitor area setLeaveMode 我的创造区 survival
```

### 第6步：测试你的区域
1. 走进区域 - 你应该自动切换到创造模式
2. 走出区域 - 你应该自动切换回生存模式
3. 你会看到屏幕上的消息确认转换

## 高级用法

### 添加玩家到白名单
白名单上的玩家不会受到区域监控影响：
```
/areamonitor whitelist add 玩家名
```

### 查看区域信息
```
/areamonitor area info 我的创造区
```

### 可视化区域边界
```
/areamonitor visual show 我的创造区
```

### 管理多个区域
你可以创建多个具有不同设置的区域：
```
/areamonitor selection create pvp竞技场
/areamonitor area setEnterMode pvp竞技场 adventure
/areamonitor area setLeaveMode pvp竞技场 survival
```

### 语言切换
```
/areamonitor language en  # 切换到英文
/areamonitor language zh  # 切换到中文
```

## 常见使用案例

### 创造建筑区
```
/areamonitor selection create 创造地块
/areamonitor area setEnterMode 创造地块 creative
/areamonitor area setLeaveMode 创造地块 survival
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
- 确保你有 OP 权限
- 确保工具在主手中
- 尝试获取新工具：`/areamonitor visual tool`

### 区域不触发
- 检查区域大小（必须 ≤1000x1000 方块）
- 确认你在正确的维度
- 检查是否在白名单上
- 查看服务器日志中的错误消息

### 语言问题
- 如果文本显示不正确，切换语言并重启
- 检查 lang 文件夹中是否两个语言文件都存在

### 性能问题
- 减少活跃区域数量
- 在配置中增加检查间隔
- 移除非常大的区域（>100000 方块）

## 提示和最佳实践

1. **从小开始**：开始时使用小区域测试功能
2. **使用描述性名称**：选择清晰、易记的区域名称
3. **彻底测试**：始终用不同游戏模式测试区域
4. **监控性能**：使用多个区域时关注服务器 TPS
5. **备份配置**：定期备份你的区域配置
6. **记录区域**：记下每个区域的用途

## 获取帮助

- 使用 `/areamonitor help` 获取命令参考
- 检查服务器日志中的错误消息
- 在 GitHub 仓库创建 issue
- 查看完整文档
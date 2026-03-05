# AreaMonitor Mod 快速开始指南

## 🚀 快速入门

### 1. 安装
1. 下载AreaMonitor模组文件
2. 放入Minecraft服务器的`mods`文件夹
3. 启动服务器

### 2. 创建第一个监控区域

#### 方法一：使用选择工具（推荐）
```
1. 获取选择工具：
   /areamonitor visual tool

2. 手持工具右键点击两个对角方块来定义区域范围

3. 创建区域：
   /areamonitor selection create my_area
```

#### 方法二：使用坐标命令
```
1. 创建区域：
   /areamonitor area create my_area

2. 设置区域范围：
   /areamonitor setArea -100 -100 100 100

3. 设置进入模式：
   /areamonitor setEnterMode creative

4. 设置离开模式：
   /areamonitor setLeaveMode survival
```

### 3. 查看区域效果
```
显示区域边界：
/areamonitor visual show my_area

查看区域信息：
/areamonitor area info my_area
```

## 🎮 进阶使用

### 创建PVP区域
```
1. 创建区域：
   /areamonitor area create pvp_zone

2. 设置大范围：
   /areamonitor setArea 500 500 700 700

3. 设置冒险模式：
   /areamonitor setEnterMode adventure
   /areamonitor setLeaveMode survival

4. 查看性能：
   /areamonitor performance
```

### 管理白名单
```
添加管理员到白名单：
/areamonitor whitelist add AdminPlayer

查看白名单：
/areamonitor whitelist list

清空白名单：
/areamonitor whitelist clear
```

### 查看限制信息
```
查看当前区域限制：
/areamonitor blacklist info
```

## ⚙️ 配置文件

### 区域配置文件
模组会在`config/areamonitor-areas.json`中保存所有区域配置。

示例配置：
```json
{
  "areas": {
    "creative_zone": {
      "displayName": "创意区域",
      "dimension": "minecraft:overworld",
      "minX": -200,
      "maxX": 200,
      "minZ": -200,
      "maxZ": 200,
      "enterMode": "creative",
      "leaveMode": "survival",
      "enabled": true,
      "whitelist": ["admin1", "admin2"]
    }
  }
}
```

### 主配置文件
主配置文件`area-monitor-common.toml`包含全局设置：
- 模组启用/禁用
- 消息显示设置
- 性能优化参数

## 🔧 故障排除

### 区域不工作
1. 检查区域是否启用：`/areamonitor area info <区域名>`
2. 确认模组已启用：`/areamonitor status`
3. 检查玩家是否在白名单中

### 性能问题
1. 查看性能监控：`/areamonitor performance`
2. 如果TPS过低，模组会自动调整检查频率
3. 考虑减小区域范围或减少区域数量

### 命令不生效
1. 确认有足够的权限（需要OP权限2级）
2. 检查命令拼写是否正确
3. 查看服务器日志是否有错误信息

## 📚 常用场景

### 场景1：创意建造区
```
/areamonitor area create creative_build
/areamonitor setArea -500 -500 500 500
/areamonitor setEnterMode creative
/areamonitor setLeaveMode survival
/areamonitor whitelist add Builder1
/areamonitor whitelist add Builder2
```

### 场景2：新手保护区
```
/areamonitor area create newbie_zone
/areamonitor setArea -200 -200 200 200
/areamonitor setEnterMode adventure
/areamonitor setLeaveMode survival
```

### 场景3：PVP竞技场
```
/areamonitor area create pvp_arena
/areamonitor setArea 1000 1000 1200 1200
/areamonitor setEnterMode adventure
/areamonitor setLeaveMode survival
```

## 🎯 最佳实践

1. **区域规划**：
   - 合理规划区域大小，避免过大影响性能
   - 不同功能的区域使用不同的游戏模式
   - 为管理员和特殊玩家设置白名单

2. **性能优化**：
   - 定期检查性能监控信息
   - 避免创建过多重叠区域
   - 及时清理不需要的区域

3. **用户体验**：
   - 使用有意义的区域名称和显示名称
   - 为玩家提供清晰的区域信息
   - 合理设置物品和传送限制

4. **安全管理**：
   - 定期备份配置文件
   - 限制普通玩家的命令权限
   - 监控异常使用情况

## 🔗 更多资源

- [完整功能文档](IMPLEMENTATION_SUMMARY.md)
- [模组主页](https://github.com/kavin-shi-source/AreaMonitor-Mod)
- 服务器日志：查看详细的运行信息
- 配置文件：手动编辑高级设置

## 🆘 获取帮助

如果遇到问题：
1. 查看服务器日志中的错误信息
2. 使用`/areamonitor help`查看所有命令
3. 检查配置文件是否正确
4. 在GitHub上提交Issue

---

**祝您使用愉快！** 🎉

如有任何问题，欢迎在项目主页提交Issue或Pull Request。
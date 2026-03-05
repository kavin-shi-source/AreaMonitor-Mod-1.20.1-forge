# 配置文件生成功能测试指南

## 测试目标
验证AreaMonitor模组的配置文件生成功能是否正常工作。

## 测试环境
- Minecraft 1.20.1
- Forge 47.0.35+
- AreaMonitor Mod 1.03+

## 测试步骤

### 1. 初始状态检查
```bash
# 检查配置目录是否存在
ls config/
# 应该没有 areamonitor 目录

# 检查配置文件
ls config/areamonitor/
# 应该显示 "No such file or directory" 或目录不存在
```

### 2. 启动服务器
```bash
# 启动Minecraft服务器
./run.sh  # 或相应的启动脚本

# 观察启动日志，应该看到以下信息：
# - "服务器启动完成，初始化配置文件..."
# - "已创建配置目录: config/areamonitor"
# - "区域配置文件不存在，创建默认配置..."
# - "黑名单配置文件不存在，创建默认配置..."
# - "配置文件初始化完成"
```

### 3. 配置文件验证
```bash
# 检查配置目录
ls config/areamonitor/
# 应该看到两个文件：
# - areas.json
# - blacklist.json

# 检查文件内容
cat config/areamonitor/areas.json
# 应该包含默认区域配置

cat config/areamonitor/blacklist.json
# 应该包含默认黑名单物品
```

### 4. 游戏内命令测试
```
# 进入游戏，以管理员身份执行以下命令：

# 测试配置文件重新加载
/areamonitor config reload
# 预期输出："§a所有配置文件已重新加载"

# 测试配置文件生成
/areamonitor config generate
# 预期输出：
# "§a配置文件已生成或验证完成"
# "§e配置文件位置:"
# "§e- 区域配置: config/areamonitor/areas.json"
# "§e- 黑名单配置: config/areamonitor/blacklist.json"

# 测试帮助命令
/areamonitor help
# 应该看到新的配置管理命令：
# "§e=== 配置管理 ==="
# "§b/areamonitor config reload §f- 重新加载所有配置文件"
# "§b/areamonitor config generate §f- 生成缺失的配置文件"
```

### 5. 区域创建测试
```
# 创建一个新的区域来测试区域配置保存
/areamonitor area create test_area
# 预期输出："§a区域 'test_area' 创建成功"

# 检查areas.json文件是否更新
cat config/areamonitor/areas.json
# 应该包含新创建的test_area区域

# 添加物品到黑名单
/areamonitor blacklist area test_area add minecraft:diamond
# 预期输出："§a已将 diamond 添加到区域 'test_area' 的黑名单"

# 检查blacklist.json文件是否更新
cat config/areamonitor/blacklist.json
# 应该包含test_area的区域特定黑名单
```

### 6. 重启测试
```bash
# 关闭服务器
# 再次启动服务器

# 验证配置文件是否正确加载
# 检查日志中是否有配置文件加载信息
# 验证之前创建的区域和配置仍然存在
```

## 预期结果

### 文件结构
```
config/
└── areamonitor/
    ├── areas.json          # 区域配置文件
    └── blacklist.json      # 黑名单配置文件
```

### areas.json 内容示例
```json
{
  "areas": {
    "default": {
      "displayName": "默认监控区域",
      "dimension": "minecraft:overworld",
      "minX": -100,
      "maxX": 100,
      "minZ": -100,
      "maxZ": 100,
      "enterMode": "adventure",
      "leaveMode": "survival",
      "enabled": true,
      "whitelist": []
    },
    "test_area": {
      "displayName": "test_area",
      "dimension": "minecraft:overworld",
      "enterMode": "adventure",
      "leaveMode": "survival",
      "enabled": true,
      "whitelist": []
    }
  }
}
```

### blacklist.json 内容示例
```json
{
  "global_blacklist": [
    "minecraft:ender_pearl",
    "minecraft:chorus_fruit",
    "minecraft:compass",
    "minecraft:clock"
  ],
  "area_blacklists": {
    "test_area": [
      "minecraft:diamond"
    ]
  }
}
```

## 故障排除

### 问题1：配置目录没有创建
**检查项：**
- 确认服务器有写权限
- 检查日志中是否有"无法创建配置目录"错误
- 确认config目录存在且有写权限

### 问题2：配置文件内容为空
**检查项：**
- 检查日志中是否有JSON序列化错误
- 确认Gson库正常工作
- 检查文件权限

### 问题3：配置没有正确加载
**检查项：**
- 检查日志中是否有配置文件加载信息
- 使用`/areamonitor config reload`命令手动重新加载
- 检查JSON文件格式是否正确

### 问题4：命令不存在
**检查项：**
- 确认模组版本是1.03+
- 检查命令注册是否成功
- 确认有适当的权限（OP权限2级）

## 成功标准

✅ 配置目录和文件在模组启动时自动创建
✅ areas.json包含默认区域配置
✅ blacklist.json包含默认黑名单物品
✅ 游戏内命令正常工作
✅ 区域创建和配置保存正常
✅ 服务器重启后配置保持不变
✅ 所有日志信息正常，没有错误

---
**测试完成时间**: [填写测试完成时间]
**测试结果**: [通过/失败]
**备注**: [记录任何异常或问题]
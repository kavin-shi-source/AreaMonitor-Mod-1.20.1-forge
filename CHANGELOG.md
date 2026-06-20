# 更新日志 - AreaMonitor Mod

[![English Version](https://img.shields.io/badge/English-Version-blue.svg)](CHANGELOG_EN.md)

## [2.0.4] - 2025-06-20

### 新增
- **GUI 管理界面**: 全新的浮动窗口式区域管理界面（Glass Morphism 主题），替代原有全屏 GUI，支持暗色世界遮罩 (v2.1.0-dev)
- **4 个子面板重写**: `AreaEditPanel`（区域编辑，含边界类型切换/保护开关/快速链接）、`WhitelistEditPanel`（白名单编辑，含新增/删除）、`RestrictionEditPanel`（限制编辑，含物品黑名单/命令限制）、`TriggerEditPanel`（触发器编辑，含命令/音效/Title/传送配置）
- **GlassButton 玻璃态按钮**: 统一风格按钮组件，全屏面使用
- **多边形选区**: 支持 3-32 个顶点的多边形区域选择，采用射线法碰撞检测（`PolygonBounds`）
- **区域保护系统**: 6 种保护类型——方块破坏/放置/交互保护、PVP 保护、爆炸保护、实体伤害保护（`ProtectionSettings` + `AreaProtectionManager`）
- **触发器系统**: 进场/离场独立触发器配置，支持执行命令、播放音效、显示 Title、跨维度传送（`TriggerConfig` + `AreaTriggerManager`）
- **模板系统**: 内置 3 个模板预设（PVP 竞技场 / 创造区 / 冒险区），支持从模板一键创建区域（`TemplateManager`）
- **Toast 反馈**: GUI 操作后显示保存/删除/创建成功提示
- **保护增强 (v2.1.0-dev)**: 新增容器交互保护（箱子/熔炉/漏斗）、流体放置保护（水/岩浆）、物品丢弃保护
- **触发器增强 (v2.1.0-dev)**: 新增 ActionBar 消息、Potion 药水效果、冷却时间 (cooldown)、延时触发 (debounce)
- **保护白名单 (v2.1.0-dev)**: 每区域独立的保护例外名单，白名单内玩家不受保护规则限制但保留游戏模式切换
- **条件触发器 (v2.1.0-dev)**: 触发器新增条件字段，支持 playerHasItem / timeRange / weather / minPlayers
- **GUI 搜索排序 (v2.1.0-dev)**: 区域列表支持名称模糊搜索和升降序排序

### 修复
- 快速链接命令多余前导斜杠修复（`sendCommand` 自动添加 `/`）
- `WhitelistEditPanel` lx 局部变量改为类字段（去重）
- Players 文字 y 坐标偏移修正
- 子面板 `onClose` 正确调用 `mainScreen.updateAfterEdit()` 刷新列表
- 面板关闭时点击外部区域关闭、列表溢出限制、按键冲突修复

### 优化
- 响应式布局：子面板控件和分区背景填满窗口宽度
- 统一分区标题栏样式、标题间距、标签渲染对齐
- Warm Parchment 暖色调主题编辑器配置
- 中文本地化 GUI 翻译全覆盖

## [2.0.3-3] - 2025-06-19

### 重构
- **命令系统拆分**: `ExtendedCommands`（原 994 行）拆分为 5 个专项命令类——`AreaCommands`（区域管理）、`WhitelistCommands`（白名单/开关/帮助/语言）、`BlacklistCommands`（黑名单+物品索引）、`VisualCommands`（可视化/性能）、`SelectionCommands`（选择工具/配置），各司其职，解除单一职责违反 (M-05)

### 优化
- **常量配置化**: `gameModeSwitchDelayMs`、`optimizationCooldownMs`、`particleSpacing`、`selectionToolItemId` 四个硬编码常量移入 `ConfigManager`，支持运行时调参无需重新编译
- **IO 初始化延后**: `ensureConfigFiles()` 从 `AreaMonitorMod` 构造函数移至 `ServerAboutToStartEvent`，消除构造阶段文件 IO 阻塞 Forge 加载 (M-01)

### 变更
- **AreaConfig 封装**: 8 个 public 字段改为 private + getter/setter，遵循封装原则 (M-08)
- **构建发布**: 启用 Gradle `publishing` 配置，支持本地 Maven 仓库发布；ForgeGradle / Parchment mappings 维持当前可用最新版 (B-03~B-05)

## [2.0.3-2] - 2025-06-19

### 清理
- **死代码移除**: 删除 `AreaMonitor.showTitle()` 方法及 `wasInArea` 字段，清理相关常量和导入 (M-03, M-04)
- **枚举清理**: 移除 `MonitorArea.BoundsType.POLYGON`（已定义但无对应实现，代码中无任何 case 分支）(M-11)
- **许可证统一**: `README.md` 许可证徽章从 MIT 更正为 GPL-3.0，与 `mods.toml` / `LICENSE.txt` 保持一致 (B-01)

### 优化
- **Lambda 提取**: `ExtendedCommands` 中 7 处重复的 `suggests` lambda 提取为 `suggestAreaNames()` 私有静态方法 (M-06)
- **空集复用**: `AreaManager.getCurrentAreas` 默认值改用 `EMPTY_AREA_SET` 常量，避免每次调用新建 `HashSet` (M-10)
- **代码风格统一**: `AreaMonitor` 中 `var players` 替换为显式类型 `List<ServerPlayer>`，与项目其余部分一致 (M-02)

### 新增
- **数据类完善**: `PlayerPosition` 添加 `equals()` / `hashCode()` 实现，消除 HashMap 键比较时的引用相等依赖 (M-09)
- **单元测试基线**: 新增 4 个核心类单元测试（AreaManager / SpatialPartitionManager / AreaBounds / PlayerPosition），建立回归保护 (B-02)

## [2.0.3-1] - 2025-06-19

### 修复
- **ConfigManager 异常处理**: 移除静态初始化块中的 `throw e`，捕获异常后回退到安全默认配置，避免类加载失败导致模组完全不可用 (CRIT-1)
- **粒子双重渲染**: 移除 `PerformanceMonitor.onServerTick` 中冗余的 `AreaVisualizer.updatePersistentVisualizations()` 调用，消除每 Tick 向客户端发送双倍粒子包的问题 (MAJ-1)
- **维度验证缺陷**: `DimensionUtils.isValidDimension` 改用 `ResourceLocation.tryParse()` 严格校验，修复 `"foo:"` 等无效格式通过验证的问题 (MAJ-5)
- **物品显示名称**: 修复 `getItemDisplayName` 返回翻译键（如 `item.minecraft.bread`）而非实际显示名称的问题，改用 `ItemStack.getHoverName()` (M-07)
- **白名单双重索引同步风险**: 白名单存储格式从 txt（纯用户名）迁移为 JSON（UUID→用户名），运行时以 UUID 为主键，消除改名导致的双索引不一致问题 (MAJ-3)

### 优化
- **白名单延迟写入**: 实现 `dirty` 标志 + 30 秒自动保存策略，替换每次修改立即写入磁盘的方式，显著减少批量操作时的 IO 开销 (MAJ-2)
- **物品补全性能**: `suggestItems` 预构建首字符索引 Map，限制建议数量上限为 100 条，避免大型模组包中遍历数万注册物品导致线程阻塞 (MAJ-4)

### 清理
- **死代码移除**: 删除 `LocalizationManager.getMinecraftLanguage()` 反射方法（无任何调用点）(SS-2)

## [2.0.3] - 2025-03-14

### 新增
- **NBT 标签识别**: 选择工具使用 NBT 标签识别，不再依赖名称，支持任意语言切换

### 修复
- **语言切换问题**: 修复切换语言后选择工具无法使用的问题
- **硬编码字符串**: 移除 SelectionTool 中的硬编码中英文字符串

### 优化
- **代码注释**: 所有代码注释统一为英文
- **日志语言**: 所有日志输出统一为英文

## [2.0.2] - 2025-03-13

### 新增
- **智能消息系统**: 自动检测客户端是否安装模组，动态选择翻译方式
- **选择工具模块**: 重构为独立类，提高代码可维护性
- **线程安全缓存**: 新增 SmartCache 类，支持并发访问

### 修复
- **线程安全**: 所有静态集合使用 ConcurrentHashMap/CopyOnWriteArrayList
- **内存泄漏**: 添加 ServerStoppingEvent 清理机制
- **配置路径**: 使用 FMLPaths API 确保配置文件正确存储
- **事件总线**: 修复双重注册问题，统一使用注解方式
- **模组图标**: 添加 logoFile 配置，修复启动器不显示图标问题
- **翻译格式化**: 修复 `{0}` 占位符不替换的问题，统一使用 `%s` 格式
- **缺失翻译键**: 添加 `command.areamonitor.area.list.header` 等缺失的翻译键
- **参数顺序**: 修复 `blacklist.item_removed` 中文翻译参数顺序错误
- **帮助命令颜色**: 统一所有帮助命令的颜色格式为金色(`§6`)命令 + 灰色(`§7`)描述
- **坐标显示**: 修复区域列表和创建区域时坐标显示不正确的问题
- **启动崩溃**: 修复 `ItemBlacklistManager` 静态初始化导致的 `Registry is already frozen` 错误

### 优化
- **代码规范**: 定义常量替代魔法数字
- **异常处理**: 添加调试日志，避免静默失败
- **国际化**: 所有注释和日志转为英文
- **代码审计**: 通过 Minecraft Forge 1.20.1 官方开发规范审计

## [2.0.0] - 2024-03-06

### 新增
- 完整中英文本地化支持
- 动态语言切换命令 `/areamonitor language en/zh`
- 持久化语言设置

### 修复
- 语言切换时工具名称不更新
- 翻译键缺失时的回退处理

### 优化
- 移除冗余代码和未使用导入
- 改进区域检查算法性能

## [1.0.3] - 2024-02-15

### 新增
- 调试模式日志
- `/areamonitor performance` 命令

### 修复
- 区域边界检测边缘情况
- 玩家断开连接处理

## [1.0.2] - 2024-01-30

### 新增
- 空间分区系统
- 区域边界缓存

### 修复
- 多人环境线程安全
- 配置文件加载问题

## [1.0.1] - 2024-01-15

### 新增
- 玩家白名单系统
- 物品黑名单功能

### 修复
- 命令注册问题
- 权限检查

## [1.0.0] - 2024-01-01

### 新增
- 首次发布
- 多区域监控与游戏模式自动切换
- 粒子效果可视化
- 完整命令系统
- 多维度支持

# ExpboostQwQ

一个强大的经验加成插件，支持玩家个人加成和全服加成，可以设置不同等级组的倍率。

## 功能特点

- 支持玩家个人经验加成
- 支持全服经验加成
- 支持设置等级组特定倍率
- 支持永久和临时加成
- 支持多语言系统
- 支持PlaceholderAPI变量
- 支持bStats统计

## PlaceholderAPI变量

本插件提供以下PlaceholderAPI变量：

- `%expboostqwq_player_multiplier%` - 显示玩家当前的个人加成倍率
- `%expboostqwq_player_duration%` - 显示玩家个人加成的剩余时间（秒）
- `%expboostqwq_server_multiplier%` - 显示当前全服加成倍率
- `%expboostqwq_server_duration%` - 显示全服加成的剩余时间（秒）
- `%expboostqwq_total_multiplier%` - 显示玩家的总倍率（包含个人、全服和等级组倍率）

## 命令

- `/expbooster help` - 显示帮助信息
- `/expbooster player <玩家名> <倍率> <时长> [选项]` - 设置玩家经验加成
- `/expbooster server <倍率> <时长> [选项]` - 设置全服经验加成
- `/expbooster check [玩家名]` - 查看加成状态
- `/expbooster global <倍率>` - 设置全局默认倍率
- `/expbooster group <等级组> <倍率> <时长>` - 设置等级组倍率
- `/expbooster groups` - 列出所有等级组及其倍率
- `/expbooster reload` - 重载插件配置
- `/expbooster logs <行数>` - 查看最近的日志
- `/expbooster logs <日期> <行数>` - 查看指定日期的日志
- `/expbooster logs list` - 列出所有日志文件
- `/expbooster logs cleanup` - 手动清理过期日志

### 选项说明

- `-levelGroup=<组名>` - 限制加成只在特定等级组生效
- `-source=<来源>` - 限制加成只对特定经验来源生效
- `-silent` - 静默模式，不发送广播消息

## 配置文件

```yaml
# ExpboostQwQ 配置文件
# 作者: LittleSheep

# 插件设置
settings:
  # 是否在经验获取时显示加成信息
  show_exp_boost_message: true

  # 全局静默模式（不发送任何消息提示）
  silent_mode: false

  # 是否启用调试模式
  debug_mode: false

  # 是否记录经验加成日志
  log_exp_boost: true

  # 是否启用bStats统计
  enable_bstats: true

  # 默认语言设置 (zh_CN: 简体中文, en_US: 英文)
  default_language: "zh_CN"

  # 日志设置
  logs:
    # 是否启用自动删除
    auto_delete: true

    # 保留日志天数（超过这个天数的日志会被删除）
    keep_days: 30

    # 检查间隔（分钟）
    check_interval: 60

    # 是否在启动时检查
    check_on_startup: true

    # 日志记录设置
    record:
      # 是否记录玩家经验获得
      exp_gain: true

      # 是否记录倍率变化
      multiplier_change: true

      # 是否记录命令执行
      command_execution: true

      # 是否记录错误信息
      errors: true

# 默认经验倍率设置
multipliers:
  # 全局默认倍率，应用于所有玩家和等级组
  global_default: 1.0

  # 等级组特定倍率，优先级高于全局默认倍率
  # 格式: 等级组名: 倍率
  level_groups:
    default: 1.0
    # 可以添加更多等级组配置
    # pvp: 1.5
    # mining: 2.0

# 玩家语言设置
# 格式: UUID: 语言代码
player_languages:
# 示例: 
# "00000000-0000-0000-0000-000000000000": "en_US"

# 语言设置
language:
  # 默认语言
  default: "zh_CN"
  # 是否允许玩家自定义语言
  allow_custom: true
```

## 权限

- `expboostqwq.command.help` - 允许使用帮助命令
- `expboostqwq.command.player` - 允许设置玩家经验加成
- `expboostqwq.command.server` - 允许设置全服经验加成
- `expboostqwq.command.check` - 允许查看经验加成状态
- `expboostqwq.admin` - 管理员权限（包含所有权限，包括日志管理）

## 统计

本插件使用bStats进行匿名统计，可以在配置文件中关闭。
[查看统计信息](https://bstats.org/plugin/bukkit/ExpboostQwQ/25432)

## 依赖

- PlaceholderAPI（可选）：用于变量支持

## 支持

如果你在使用过程中遇到任何问题，欢迎提出Issue或加入我们的交流群。

## 系统要求

- Java 8 或更高版本
- Spigot/Paper 1.12+ 服务端
- AkariLevel 插件

## 安装说明

1. 下载最新版本的 ExpboostQwQ.jar
2. 将jar文件放入服务器的 plugins 目录
3. 重启服务器或重载插件
4. 插件将自动生成配置文件

## 语言设置命令

- `/expbooster language [语言代码]` - 设置或查看你的语言
- `/expbooster language list` - 列出所有可用的语言
- `/expbooster language player <玩家名> <语言代码>` - 设置玩家的语言（管理员）
- `/expbooster language server <语言代码>` - 设置服务器默认语言（管理员）

## 开发信息

- 作者：LittleSheep
- 版本：1.0.0-SNAPSHOT
- 许可证：MIT

## 构建项目

```bash
./gradlew shadowJar
```

构建后的插件文件将位于 `build/libs` 目录下。

## 插件使用统计

![bStats 服务器数量](https://bstats.org/signatures/bukkit/ExpboostQwQ.svg)

您可以在[这里](https://bstats.org/plugin/bukkit/ExpboostQwQ/25432)查看更详细的统计信息。

## 未来规划

以下是插件的未来开发计划：

### 近期计划 (v1.1.0)
- [ ] 添加GUI界面支持，使操作更加直观
- [ ] 支持MySQL数据库存储
- [ ] 添加更多经验来源的细分控制
- [ ] 优化性能，减少内存占用

### 中期计划 (v1.2.0)
- [ ] 添加经验加成活动系统
- [ ] 支持更多变量和条件判断
- [ ] 添加经验加成道具系统
- [ ] 支持Redis缓存

### 长期计划 (v2.0.0)
- [ ] 重构代码架构，提升扩展性
- [ ] 添加API支持，方便其他插件集成
- [ ] 支持更多服务端版本
- [ ] 添加网页管理界面
- [ ] 支持更多数据统计和分析功能

## 问题反馈

如果您在使用过程中遇到任何问题，或有好的建议，欢迎通过以下方式反馈：

1. 在GitHub上提交Issue
2. 加入我们的交流群：[点击加入](https://jq.qq.com/?_wv=1027&k=xxxxx)
3. 发送邮件至：support@example.com

## 赞助支持

如果您觉得这个插件对您有帮助，欢迎赞助支持我们的开发工作！

- 爱发电：[点击赞助](https://afdian.net/xxxxx)
- PayPal：[点击赞助](https://paypal.me/xxxxx)

所有赞助者都将获得：
- 专属标识
- 优先技术支持
- 新功能建议优先采纳
- 提前体验新版本

## 鸣谢
感谢以下项目和贡献者：
- AkariLevel - 等级系统支持
- PlaceholderAPI - 变量支持
- bStats - 统计支持
- 所有提供反馈和建议的用户 
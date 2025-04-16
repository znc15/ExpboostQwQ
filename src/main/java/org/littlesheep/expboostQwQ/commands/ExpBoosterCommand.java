package org.littlesheep.expboostQwQ.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.littlesheep.expboostQwQ.ExpboostQwQ;
import org.littlesheep.expboostQwQ.data.PlayerBooster;
import org.littlesheep.expboostQwQ.data.ServerBooster;
import org.littlesheep.expboostQwQ.utils.LevelApiUtil;
import org.littlesheep.expboostQwQ.utils.LogUtil;
import org.littlesheep.expboostQwQ.utils.TimeUtils;
import org.littlesheep.expboostQwQ.utils.LanguageManager;
import org.littlesheep.expboostQwQ.utils.UpdateChecker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 经验加成命令处理器
 */
public class ExpBoosterCommand implements CommandExecutor, TabCompleter {
    
    // 插件实例引用
    private final ExpboostQwQ plugin;
    
    /**
     * 构造函数
     * @param plugin 插件主类实例
     */
    public ExpBoosterCommand(ExpboostQwQ plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 命令执行处理
     * 
     * @param sender 命令发送者
     * @param command 命令对象
     * @param label 使用的命令标签
     * @param args 命令参数
     * @return 命令是否成功处理
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendHelpMessage(sender);
            LogUtil.commandExecution(sender.getName(), "/" + label, false);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        String fullCommand = "/" + label + " " + String.join(" ", args);
        
        boolean success = true;
        try {
            switch (subCommand) {
                case "help":
                    sendHelpMessage(sender);
                    break;
                case "player":
                    handlePlayerBooster(sender, args);
                    break;
                case "server":
                    handleServerBooster(sender, args);
                    break;
                case "check":
                    handleCheck(sender, args);
                    break;
                case "global":
                    handleGlobalMultiplier(sender, args);
                    break;
                case "group":
                    handleGroupMultiplier(sender, args);
                    break;
                case "groups":
                    handleListGroups(sender);
                    break;
                case "language":
                    handleLanguage(sender, args);
                    break;
                case "reload":
                    handleReload(sender);
                    break;
                case "logs":
                    if (args.length > 1 && args[1].equalsIgnoreCase("cleanup")) {
                        handleLogsCleanup(sender);
                    } else {
                        handleLogs(sender, args);
                    }
                    break;
                case "disable":
                    if (args.length > 1) {
                        handleDisable(sender, args);
                    } else {
                        sendHelpMessage(sender);
                        success = false;
                    }
                    break;
                case "checkupdate":
                    handleCheckUpdate(sender);
                    break;
                default:
                    sendHelpMessage(sender);
                    success = false;
                    break;
            }
        } catch (Exception e) {
            success = false;
            LogUtil.error("执行命令时发生错误: " + fullCommand, e);
        }
        
        LogUtil.commandExecution(sender.getName(), fullCommand, success);
        return true;
    }
    
    /**
     * 处理玩家经验加成子命令
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handlePlayerBooster(CommandSender sender, String[] args) {
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
        
        if (!sender.hasPermission("expboostqwq.command.player")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.no_permission",
                    "§c[ExpboostQwQ] 你没有权限执行此命令!"));
            return;
        }
        
        if (args.length < 4) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.usage.player",
                    "§c[ExpboostQwQ] 用法: /expbooster player <玩家名> <倍率> <时长> [选项]"));
            return;
        }
        
        // 获取玩家
        String playerName = args[1];
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.player_not_found",
                    "§c[ExpboostQwQ] 找不到玩家: %player%")
                    .replace("%player%", playerName));
            return;
        }
        
        // 解析倍率
        double multiplier;
        try {
            multiplier = Double.parseDouble(args[2]);
            if (multiplier <= 0) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.command.invalid_multiplier",
                        "§c[ExpboostQwQ] 倍率必须大于0!"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.invalid_number",
                    "§c[ExpboostQwQ] 倍率必须是有效的数字!"));
            return;
        }
        
        // 解析时长
        long duration;
        if (args[3].equalsIgnoreCase("permanent") || args[3].equalsIgnoreCase("perm") || args[3].equals("-1")) {
            duration = -1; // 永久
        } else {
            try {
                duration = TimeUtils.parseDuration(args[3]);
                if (duration <= 0) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.command.invalid_duration",
                            "§c[ExpboostQwQ] 时长必须大于0或为'permanent'!"));
                    return;
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.command.invalid_duration",
                        "§c[ExpboostQwQ] 无效的时间格式! 使用例如 1d12h30m 或 'permanent'"));
                return;
            }
        }
        
        // 处理可选参数
        String levelGroup = "";
        String source = "";
        boolean silent = false;
        
        for (int i = 4; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.startsWith("-levelGroup=")) {
                levelGroup = arg.substring("-levelGroup=".length());
                // 验证等级组是否存在
                if (!levelGroup.isEmpty() && !LevelApiUtil.isLevelGroupExists(levelGroup)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.command.invalid_level_group",
                            "§c[ExpboostQwQ] 警告: 等级组 '%group%' 不存在，将应用于所有等级组")
                            .replace("%group%", levelGroup));
                }
            } else if (arg.startsWith("-source=")) {
                source = arg.substring("-source=".length());
            } else if (arg.equals("-silent")) {
                silent = true;
            }
        }
        
        // 计算结束时间
        long endTime = (duration == -1) ? -1 : System.currentTimeMillis() + (duration * 1000);
        
        // 在创建并应用加成之前记录旧的倍率
        double oldMultiplier = 1.0;
        if (plugin.getBoosterManager().hasPlayerBooster(target.getUniqueId())) {
            PlayerBooster oldBooster = plugin.getBoosterManager().getPlayerBooster(target.getUniqueId());
            if (oldBooster != null) {
                oldMultiplier = oldBooster.getMultiplier();
            }
        }
        
        // 创建并应用加成
        PlayerBooster booster = new PlayerBooster(multiplier, endTime, levelGroup, source);
        plugin.getBoosterManager().addPlayerBooster(target.getUniqueId(), booster);
        
        // 记录倍率变化
        LogUtil.multiplierChange(target, oldMultiplier, multiplier, duration, "个人");
        
        // 发送确认消息
        String durationStr = (duration == -1) ? "永久" : TimeUtils.formatDuration(duration);
        
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.exp_boost.player_booster_set",
                "§a[ExpboostQwQ] 已为玩家 §e%player% §a设置 §e%multiplier%x §a经验加成，持续时间: §e%duration%")
                .replace("%player%", target.getName())
                .replace("%multiplier%", String.valueOf(multiplier))
                .replace("%duration%", durationStr));
        
        if (!silent && !sender.equals(target)) {
            String targetLangCode = plugin.getLanguageManager().getPlayerLanguage(target.getUniqueId().toString());
            target.sendMessage(plugin.getLanguageManager().getMessage(
                    targetLangCode,
                    "messages.exp_boost.player_booster_received",
                    "§a[ExpboostQwQ] 你获得了 §e%multiplier%x §a经验加成，持续时间: §e%duration%")
                    .replace("%multiplier%", String.valueOf(multiplier))
                    .replace("%duration%", durationStr));
        }
        
        // 记录日志
        LogUtil.debug("管理员 " + sender.getName() + " 为玩家 " + target.getName() + 
                " 设置了 " + multiplier + "x 经验加成，持续时间: " + durationStr);
    }
    
    /**
     * 处理全服经验加成子命令
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleServerBooster(CommandSender sender, String[] args) {
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
        
        if (!sender.hasPermission("expboostqwq.command.server")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.no_permission",
                    "§c[ExpboostQwQ] 你没有权限执行此命令!"));
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.usage.server",
                    "§c[ExpboostQwQ] 用法: /expbooster server <倍率> <时长> [选项]"));
            return;
        }
        
        // 解析倍率
        double multiplier;
        try {
            multiplier = Double.parseDouble(args[1]);
            if (multiplier <= 0) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.command.invalid_multiplier",
                        "§c[ExpboostQwQ] 倍率必须大于0!"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.invalid_number",
                    "§c[ExpboostQwQ] 倍率必须是有效的数字!"));
            return;
        }
        
        // 解析时长
        long duration;
        if (args[2].equalsIgnoreCase("permanent") || args[2].equalsIgnoreCase("perm") || args[2].equals("-1")) {
            duration = -1; // 永久
        } else {
            try {
                duration = TimeUtils.parseDuration(args[2]);
                if (duration <= 0) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.command.invalid_duration",
                            "§c[ExpboostQwQ] 时长必须大于0或为'permanent'!"));
                    return;
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.command.invalid_duration",
                        "§c[ExpboostQwQ] 无效的时间格式! 使用例如 1d12h30m 或 'permanent'"));
                return;
            }
        }
        
        // 处理可选参数
        String levelGroup = "";
        String source = "";
        boolean silent = false;
        
        for (int i = 3; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.startsWith("-levelGroup=")) {
                levelGroup = arg.substring("-levelGroup=".length());
                // 验证等级组是否存在
                if (!levelGroup.isEmpty() && !LevelApiUtil.isLevelGroupExists(levelGroup)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.command.invalid_level_group",
                            "§c[ExpboostQwQ] 警告: 等级组 '%group%' 不存在，将应用于所有等级组")
                            .replace("%group%", levelGroup));
                }
            } else if (arg.startsWith("-source=")) {
                source = arg.substring("-source=".length());
            } else if (arg.equals("-silent")) {
                silent = true;
            }
        }
        
        // 计算结束时间
        long endTime = (duration == -1) ? -1 : System.currentTimeMillis() + (duration * 1000);
        
        // 在创建并应用加成之前记录旧的倍率
        double oldMultiplier = 1.0;
        ServerBooster oldBooster = plugin.getBoosterManager().getServerBooster();
        if (oldBooster != null) {
            oldMultiplier = oldBooster.getMultiplier();
        }
        
        // 创建并应用加成
        ServerBooster booster = new ServerBooster(multiplier, endTime, levelGroup, source);
        plugin.getBoosterManager().setServerBooster(booster);
        
        // 记录倍率变化
        LogUtil.multiplierChange(null, oldMultiplier, multiplier, duration, "全服");
        
        // 发送确认消息
        String durationStr = (duration == -1) ? "永久" : TimeUtils.formatDuration(duration);
        
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.exp_boost.server_booster_set",
                "§a[ExpboostQwQ] 已为全服设置 §e%multiplier%x §a经验加成，持续时间: §e%duration%")
                .replace("%multiplier%", String.valueOf(multiplier))
                .replace("%duration%", durationStr));
        
        if (!silent) {
            String broadcastMessage = plugin.getLanguageManager().getMessage(
                    plugin.getLanguageManager().getDefaultLanguage(),
                    "messages.exp_boost.server_booster_broadcast",
                    "§a[ExpboostQwQ] 全服获得了 §e%multiplier%x §a经验加成，持续时间: §e%duration%")
                    .replace("%multiplier%", String.valueOf(multiplier))
                    .replace("%duration%", durationStr);
            Bukkit.broadcastMessage(broadcastMessage);
        }
        
        // 记录日志
        LogUtil.debug("管理员 " + sender.getName() + " 设置了全服 " + multiplier + "x 经验加成，持续时间: " + durationStr);
    }
    
    /**
     * 处理查看加成子命令
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleCheck(CommandSender sender, String[] args) {
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
        
        if (!sender.hasPermission("expboostqwq.command.check")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.no_permission",
                    "§c[ExpboostQwQ] 你没有权限执行此命令!"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.usage.check",
                    "§c[ExpboostQwQ] 用法: /expbooster check [玩家名]"));
            return;
        }
        
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.player_not_found",
                    "§c[ExpboostQwQ] 找不到玩家 %player%!")
                    .replace("%player%", targetName));
            return;
        }
        
        UUID targetUUID = target.getUniqueId();
        PlayerBooster booster = plugin.getBoosterManager().getPlayerBooster(targetUUID);
        ServerBooster serverBooster = plugin.getBoosterManager().getServerBooster();
        
        // 显示玩家个人加成信息
        if (booster != null && booster.isActive()) {
            String endTimeStr = booster.getEndTime() == -1 ? "永久" : 
                    TimeUtils.formatDuration((booster.getEndTime() - System.currentTimeMillis()) / 1000);
            
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.check_player",
                    "§a[ExpboostQwQ] 玩家 §e%player% §a的经验加成: §e%multiplier%x§a, 剩余时间: §e%time%")
                    .replace("%player%", target.getName())
                    .replace("%multiplier%", String.valueOf(booster.getMultiplier()))
                    .replace("%time%", endTimeStr));
            
            if (!booster.getLevelGroup().isEmpty()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.exp_boost.check_level_group",
                        "§a[ExpboostQwQ] 等级组: §e%group%")
                        .replace("%group%", booster.getLevelGroup()));
            }
            
            if (!booster.getSource().isEmpty()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.exp_boost.check_source",
                        "§a[ExpboostQwQ] 来源: §e%source%")
                        .replace("%source%", booster.getSource()));
            }
        } else {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.no_player_booster",
                    "§a[ExpboostQwQ] 玩家 §e%player% §a当前没有个人经验加成")
                    .replace("%player%", target.getName()));
        }
        
        // 显示全服加成信息
        if (serverBooster != null && serverBooster.isActive()) {
            String endTimeStr = serverBooster.getEndTime() == -1 ? "永久" : 
                    TimeUtils.formatDuration((serverBooster.getEndTime() - System.currentTimeMillis()) / 1000);
            
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.check_server",
                    "§a[ExpboostQwQ] 全服经验加成: §e%multiplier%x§a, 剩余时间: §e%time%")
                    .replace("%multiplier%", String.valueOf(serverBooster.getMultiplier()))
                    .replace("%time%", endTimeStr));
            
            if (!serverBooster.getLevelGroup().isEmpty()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.exp_boost.check_level_group",
                        "§a[ExpboostQwQ] 等级组: §e%group%")
                        .replace("%group%", serverBooster.getLevelGroup()));
            }
            
            if (!serverBooster.getSource().isEmpty()) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.exp_boost.check_source",
                        "§a[ExpboostQwQ] 来源: §e%source%")
                        .replace("%source%", serverBooster.getSource()));
            }
        } else {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.no_server_booster",
                    "§a[ExpboostQwQ] 当前没有全服经验加成"));
        }
        
        // 显示全局默认倍率或等级组倍率
        double globalOrGroupMultiplier = 1.0;
        String levelGroup = ""; // 假设玩家当前没有指定的等级组
        
        // 如果插件支持检测玩家当前的等级组，可以在这里获取
        // 例如: levelGroup = LevelApiUtil.getPlayerLevelGroup(target);
        
        if (!levelGroup.isEmpty() && plugin.getBoosterManager().hasLevelGroupBooster(levelGroup)) {
            globalOrGroupMultiplier = plugin.getBoosterManager().getLevelGroupMultiplier(levelGroup);
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.check_level_group_multiplier",
                    "§a[ExpboostQwQ] 等级组 §e%group% §a的倍率: §e%multiplier%x")
                    .replace("%group%", levelGroup)
                    .replace("%multiplier%", String.valueOf(globalOrGroupMultiplier)));
        } else {
            globalOrGroupMultiplier = plugin.getBoosterManager().getGlobalDefaultMultiplier();
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.check_global_multiplier",
                    "§a[ExpboostQwQ] 全局默认倍率: §e%multiplier%x")
                    .replace("%multiplier%", String.valueOf(globalOrGroupMultiplier)));
        }
        
        // 显示加成计算方式
        String calculationType = plugin.getConfig().getString("settings.boost_calculation", "multiply");
        String calcTypeName;
        
        if (calculationType.equalsIgnoreCase("highest")) {
            calcTypeName = "取最高倍率";
        } else if (calculationType.equalsIgnoreCase("add")) {
            calcTypeName = "相加模式";
        } else {
            calcTypeName = "相乘模式";
        }
        
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.exp_boost.check_calculation_type",
                "§a[ExpboostQwQ] 加成计算方式: §e%type%")
                .replace("%type%", calcTypeName));
        
        // 显示总倍率
        Player targetPlayer = Bukkit.getPlayer(targetUUID);
        if (targetPlayer != null) {
            // 获取各个倍率
            double playerMultiplier = (booster != null && booster.isActive()) ? booster.getMultiplier() : 1.0;
            double serverMultiplier = (serverBooster != null && serverBooster.isActive()) ? serverBooster.getMultiplier() : 1.0;
            
            // 根据不同计算方式显示倍率计算过程
            double totalMultiplier = plugin.getBoosterManager().getEffectiveMultiplier(targetPlayer, "", "");
            
            if (calculationType.equalsIgnoreCase("highest")) {
                // 取最高倍率
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.exp_boost.total_multiplier_highest",
                        "§a[ExpboostQwQ] 三种倍率: §e全局/组(%global%)§a, §e服务器(%server%)§a, §e玩家(%player%)§a")
                        .replace("%global%", String.valueOf(globalOrGroupMultiplier))
                        .replace("%server%", String.valueOf(serverMultiplier))
                        .replace("%player%", String.valueOf(playerMultiplier)));
                
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.exp_boost.total_multiplier_calculation",
                        "§a[ExpboostQwQ] 计算方式: §f取最高 = §e%multiplier%x")
                        .replace("%multiplier%", String.valueOf(totalMultiplier)));
                
            } else if (calculationType.equalsIgnoreCase("add")) {
                // 相加模式
                double addResult = 1.0 + (globalOrGroupMultiplier - 1.0) + (serverMultiplier - 1.0) + (playerMultiplier - 1.0);
                
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.exp_boost.total_multiplier_add",
                        "§a[ExpboostQwQ] 计算方式: §f1.0 + (%global% - 1.0) + (%server% - 1.0) + (%player% - 1.0) = §e%multiplier%x")
                        .replace("%global%", String.valueOf(globalOrGroupMultiplier))
                        .replace("%server%", String.valueOf(serverMultiplier))
                        .replace("%player%", String.valueOf(playerMultiplier))
                        .replace("%multiplier%", String.valueOf(addResult)));
                
            } else {
                // 相乘模式
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.exp_boost.total_multiplier_multiply",
                        "§a[ExpboostQwQ] 计算方式: §f%global% × %server% × %player% = §e%multiplier%x")
                        .replace("%global%", String.valueOf(globalOrGroupMultiplier))
                        .replace("%server%", String.valueOf(serverMultiplier))
                        .replace("%player%", String.valueOf(playerMultiplier))
                        .replace("%multiplier%", String.valueOf(totalMultiplier)));
            }
            
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.total_multiplier",
                    "§a[ExpboostQwQ] 总经验倍率: §e%multiplier%x")
                    .replace("%multiplier%", String.valueOf(totalMultiplier)));
        }
    }
    
    /**
     * 处理全局默认倍率设置命令
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleGlobalMultiplier(CommandSender sender, String[] args) {
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
        
        if (!sender.hasPermission("expboostqwq.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.no_permission",
                    "§c[ExpboostQwQ] 你没有权限执行此命令!"));
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.usage.global",
                    "§c[ExpboostQwQ] 用法: /expbooster global <倍率> <时长> [选项]"));
            return;
        }
        
        // 解析倍率
        double multiplier;
        try {
            multiplier = Double.parseDouble(args[1]);
            if (multiplier <= 0) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.command.invalid_multiplier",
                        "§c[ExpboostQwQ] 倍率必须大于0!"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.invalid_number",
                    "§c[ExpboostQwQ] 倍率必须是有效的数字!"));
            return;
        }
        
        // 解析时长
        long duration;
        if (args[2].equalsIgnoreCase("permanent") || args[2].equalsIgnoreCase("perm") || args[2].equals("-1")) {
            duration = -1; // 永久
        } else {
            try {
                duration = TimeUtils.parseDuration(args[2]);
                if (duration <= 0) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.command.invalid_duration",
                            "§c[ExpboostQwQ] 时长必须大于0或为'permanent'!"));
                    return;
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.command.invalid_duration",
                        "§c[ExpboostQwQ] 无效的时间格式! 使用例如 1d12h30m 或 'permanent'"));
                return;
            }
        }
        
        // 处理可选参数
        String levelGroup = "";
        String source = "";
        boolean silent = false;
        
        for (int i = 3; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.startsWith("-levelGroup=")) {
                levelGroup = arg.substring("-levelGroup=".length());
                // 验证等级组是否存在
                if (!levelGroup.isEmpty() && !LevelApiUtil.isLevelGroupExists(levelGroup)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.command.invalid_level_group",
                            "§c[ExpboostQwQ] 警告: 等级组 '%group%' 不存在，将应用于所有等级组")
                            .replace("%group%", levelGroup));
                }
            } else if (arg.startsWith("-source=")) {
                source = arg.substring("-source=".length());
            } else if (arg.equals("-silent")) {
                silent = true;
            }
        }
        
        // 设置全局默认倍率
        plugin.getBoosterManager().setGlobalDefaultMultiplier(multiplier, duration);
        
        // 发送确认消息
        String durationStr = (duration == -1) ? "永久" : TimeUtils.formatDuration(duration);
        String message = plugin.getLanguageManager().getMessage(
                langCode,
                "messages.exp_boost.global_multiplier_set", 
                "§a[ExpboostQwQ] 全局默认经验倍率已设置为 §e%multiplier%x§a，持续时间: §e%duration%")
                .replace("%multiplier%", String.valueOf(multiplier))
                .replace("%duration%", durationStr);
        
        // 添加选项信息
        if (!levelGroup.isEmpty()) {
            message += plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.level_group_restriction",
                    "\n§7- 限制等级组: §e%group%")
                    .replace("%group%", levelGroup);
        }
        if (!source.isEmpty()) {
            message += plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.source_restriction",
                    "\n§7- 限制来源: §e%source%")
                    .replace("%source%", source);
        }
        
        sender.sendMessage(message);
        
        // 如果不是静默模式，广播消息
        if (!silent) {
            String broadcastMessage = plugin.getLanguageManager().getMessage(
                    plugin.getLanguageManager().getDefaultLanguage(),
                    "messages.exp_boost.global_multiplier_broadcast",
                    "§a[ExpboostQwQ] 全局默认经验倍率已设置为 §e%multiplier%x§a，持续时间: §e%duration%")
                    .replace("%multiplier%", String.valueOf(multiplier))
                    .replace("%duration%", durationStr);
            Bukkit.broadcastMessage(broadcastMessage);
        }
        
        // 记录日志
        LogUtil.debug("管理员 " + sender.getName() + " 设置全局默认倍率为 " + multiplier + "x，持续时间: " + durationStr +
                (levelGroup.isEmpty() ? "" : "，限制等级组: " + levelGroup) +
                (source.isEmpty() ? "" : "，限制来源: " + source));
    }
    
    /**
     * 处理等级组倍率设置命令
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleGroupMultiplier(CommandSender sender, String[] args) {
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
        
        if (!sender.hasPermission("expboostqwq.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.no_permission",
                    "§c[ExpboostQwQ] 你没有权限执行此命令!"));
            return;
        }
        
        if (args.length < 4) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.usage.group",
                    "§c[ExpboostQwQ] 用法: /expbooster group <等级组> <倍率> <时长> [选项]"));
            return;
        }
        
        // 获取等级组名称
        String levelGroup = args[1];
        
        // 验证等级组是否存在
        if (!LevelApiUtil.isLevelGroupExists(levelGroup)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.level_group_not_exists",
                    "§c[ExpboostQwQ] 错误: 等级组 '%group%' 不存在")
                    .replace("%group%", levelGroup));
            
            String availableGroups = String.join(", ", LevelApiUtil.getLevelGroupNames());
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.available_level_groups",
                    "§c[ExpboostQwQ] 可用的等级组: %groups%")
                    .replace("%groups%", availableGroups));
            return;
        }
        
        // 解析倍率
        double multiplier;
        try {
            multiplier = Double.parseDouble(args[2]);
            if (multiplier <= 0) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.command.invalid_multiplier",
                        "§c[ExpboostQwQ] 倍率必须大于0!"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.invalid_number",
                    "§c[ExpboostQwQ] 倍率必须是有效的数字!"));
            return;
        }
        
        // 解析时长
        long duration;
        if (args[3].equalsIgnoreCase("permanent") || args[3].equalsIgnoreCase("perm") || args[3].equals("-1")) {
            duration = -1; // 永久
        } else {
            try {
                duration = TimeUtils.parseDuration(args[3]);
                if (duration <= 0) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.command.invalid_duration",
                            "§c[ExpboostQwQ] 时长必须大于0或为'permanent'!"));
                    return;
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.command.invalid_duration",
                        "§c[ExpboostQwQ] 无效的时间格式! 使用例如 1d12h30m 或 'permanent'"));
                return;
            }
        }
        
        // 处理可选参数
        String targetLevelGroup = "";
        String source = "";
        boolean silent = false;
        
        for (int i = 4; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.startsWith("-levelGroup=")) {
                targetLevelGroup = arg.substring("-levelGroup=".length());
                // 验证等级组是否存在
                if (!targetLevelGroup.isEmpty() && !LevelApiUtil.isLevelGroupExists(targetLevelGroup)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.command.invalid_level_group",
                            "§c[ExpboostQwQ] 警告: 等级组 '%group%' 不存在，将应用于所有等级组")
                            .replace("%group%", targetLevelGroup));
                }
            } else if (arg.startsWith("-source=")) {
                source = arg.substring("-source=".length());
            } else if (arg.equals("-silent")) {
                silent = true;
            }
        }
        
        // 设置等级组倍率
        if (plugin.getBoosterManager().setLevelGroupMultiplier(levelGroup, multiplier, duration)) {
            // 发送确认消息
            String durationStr = (duration == -1) ? "永久" : TimeUtils.formatDuration(duration);
            String message = plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.group_multiplier_set", 
                    "§a[ExpboostQwQ] 等级组 §e%group% §a的经验倍率已设置为 §e%multiplier%x§a，持续时间: §e%duration%")
                    .replace("%group%", levelGroup)
                    .replace("%multiplier%", String.valueOf(multiplier))
                    .replace("%duration%", durationStr);
            
            // 添加选项信息
            if (!targetLevelGroup.isEmpty()) {
                message += plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.exp_boost.level_group_restriction",
                        "\n§7- 限制等级组: §e%group%")
                        .replace("%group%", targetLevelGroup);
            }
            if (!source.isEmpty()) {
                message += plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.exp_boost.source_restriction",
                        "\n§7- 限制来源: §e%source%")
                        .replace("%source%", source);
            }
            
            sender.sendMessage(message);
            
            // 如果不是静默模式，广播消息
            if (!silent) {
                String broadcastMessage = plugin.getLanguageManager().getMessage(
                        plugin.getLanguageManager().getDefaultLanguage(),
                        "messages.exp_boost.group_multiplier_broadcast",
                        "§a[ExpboostQwQ] 等级组 §e%group% §a获得了 §e%multiplier%x §a经验加成，持续时间: §e%duration%")
                        .replace("%group%", levelGroup)
                        .replace("%multiplier%", String.valueOf(multiplier))
                        .replace("%duration%", durationStr);
                Bukkit.broadcastMessage(broadcastMessage);
            }
            
            // 记录日志
            LogUtil.debug("管理员 " + sender.getName() + " 将等级组 " + levelGroup + " 的倍率设置为 " + multiplier + "x，持续时间: " + durationStr +
                    (targetLevelGroup.isEmpty() ? "" : "，限制等级组: " + targetLevelGroup) +
                    (source.isEmpty() ? "" : "，限制来源: " + source));
        }
    }
    
    /**
     * 处理列出所有等级组命令
     * 
     * @param sender 命令发送者
     */
    private void handleListGroups(CommandSender sender) {
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
        
        if (!sender.hasPermission("expboostqwq.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.no_permission",
                    "§c[ExpboostQwQ] 你没有权限执行此命令!"));
            return;
        }
        
        // 获取所有等级组
        ArrayList<String> groups = LevelApiUtil.getLevelGroupNames();
        
        // 获取所有等级组倍率
        Map<String, PlayerBooster> groupBoosters = plugin.getBoosterManager().getAllLevelGroupBoosters();
        double globalMultiplier = plugin.getBoosterManager().getGlobalDefaultMultiplier();
        
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.command.groups_title",
                "§a[ExpboostQwQ] 可用的等级组列表和倍率设置:"));
                
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.check.global_multiplier_info",
                "§a[ExpboostQwQ] 全局默认经验倍率: §e%multiplier%x")
                .replace("%multiplier%", String.valueOf(globalMultiplier)));
        
        if (groups.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.no_groups_found",
                    "§c[ExpboostQwQ] 没有找到任何等级组"));
        } else {
            for (String group : groups) {
                PlayerBooster booster = groupBoosters.get(group);
                if (booster != null && booster.isActive()) {
                    String durationStr = booster.getEndTime() == -1 ? "永久" : 
                            TimeUtils.formatDuration((booster.getEndTime() - System.currentTimeMillis()) / 1000);
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.check.group_multiplier_detail",
                            "§7- §f%group%: §e%multiplier%x §7(剩余时间: %duration%)")
                            .replace("%group%", group)
                            .replace("%multiplier%", String.valueOf(booster.getMultiplier()))
                            .replace("%duration%", durationStr));
                } else {
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.check.group_multiplier_detail",
                            "§7- §f%group%: §e%multiplier%x")
                            .replace("%group%", group)
                            .replace("%multiplier%", String.valueOf(globalMultiplier)));
                }
            }
        }
    }
    
    /**
     * 处理重载插件配置命令
     * 
     * @param sender 命令发送者
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("expboostqwq.admin")) {
            sender.sendMessage("§c你没有权限执行此命令!");
            return;
        }
        
        // 执行重载
        plugin.reload();
        
        // 发送确认消息
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
                
        String message = plugin.getLanguageManager().getMessage(
                langCode,
                "messages.plugin.reload", 
                "§a[ExpboostQwQ] 插件配置已重载！");
        sender.sendMessage(message);
        
        // 记录日志
        LogUtil.debug("管理员 " + sender.getName() + " 重载了插件配置");
    }
    
    /**
     * 处理日志查询命令
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleLogs(CommandSender sender, String[] args) {
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
        
        if (!sender.hasPermission("expboostqwq.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.no_permission",
                    "§c[ExpboostQwQ] 你没有权限执行此命令!"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.usage.logs",
                    "§c[ExpboostQwQ] 用法:\n" +
                    "§c/expbooster logs <行数> - 查看最近的日志\n" +
                    "§c/expbooster logs <日期> <行数> - 查看指定日期的日志\n" +
                    "§c/expbooster logs list - 列出所有日志文件"));
            return;
        }
        
        if (args[1].equalsIgnoreCase("list")) {
            // 列出所有日志文件
            File[] logFiles = LogUtil.getLogFiles();
            if (logFiles.length == 0) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.logs.no_files",
                        "§c[ExpboostQwQ] 没有找到任何日志文件"));
                return;
            }
            
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.logs.available_files",
                    "§a[ExpboostQwQ] 可用的日志文件:"));
            
            for (File file : logFiles) {
                String fileName = file.getName();
                sender.sendMessage("§7- §f" + fileName.substring(0, fileName.length() - 4));
            }
            return;
        }
        
        // 解析行数和日期
        int lines = 10; // 默认显示10行
        String date = null;
        
        if (args.length >= 3) {
            // 格式: /expbooster logs <日期> <行数>
            date = args[1];
            try {
                lines = Integer.parseInt(args[2]);
                if (lines <= 0) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.logs.invalid_lines",
                            "§c[ExpboostQwQ] 行数必须大于0"));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.logs.invalid_number",
                        "§c[ExpboostQwQ] 无效的行数"));
                return;
            }
        } else {
            // 格式: /expbooster logs <行数>
            try {
                lines = Integer.parseInt(args[1]);
                if (lines <= 0) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            langCode,
                            "messages.logs.invalid_lines",
                            "§c[ExpboostQwQ] 行数必须大于0"));
                    return;
                }
            } catch (NumberFormatException e) {
                // 可能是日期格式
                date = args[1];
                lines = 10; // 使用默认行数
            }
        }
        
        // 读取并显示日志
        String[] logs;
        if (date != null) {
            logs = LogUtil.getLogsFromDate(date, lines);
        } else {
            logs = LogUtil.getRecentLogs(lines);
        }
        
        if (logs.length == 0) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.logs.no_logs",
                    "§c[ExpboostQwQ] 没有找到任何日志"));
            return;
        }
        
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.logs.header",
                "§6======= §e日志内容 §6======="));
        
        for (String log : logs) {
            sender.sendMessage("§7" + log);
        }
        
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.logs.footer",
                "§6===================="));
    }
    
    /**
     * 处理日志清理命令
     * @param sender 命令发送者
     */
    private void handleLogsCleanup(CommandSender sender) {
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
        
        if (!sender.hasPermission("expboostqwq.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.no_permission",
                    "§c[ExpboostQwQ] 你没有权限执行此命令!"));
            return;
        }
        
        int deletedFiles = LogUtil.cleanupLogs();
        
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.logs.cleanup_complete",
                "§a[ExpboostQwQ] 日志清理完成，共删除 §e%count% §a个日志文件")
                .replace("%count%", String.valueOf(deletedFiles)));
    }
    
    /**
     * 发送帮助信息
     * 
     * @param sender 命令发送者
     */
    private void sendHelpMessage(CommandSender sender) {
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
        
        LanguageManager lang = plugin.getLanguageManager();
        
        sender.sendMessage(lang.getMessage(langCode, "messages.help.header", "§6======= §eExpboostQwQ 帮助 §6======="));
        sender.sendMessage(lang.getMessage(langCode, "messages.help.help", "§e/expbooster help §7- §f显示此帮助信息"));
        
        if (sender.hasPermission("expboostqwq.command.player")) {
            sender.sendMessage(lang.getMessage(langCode, "messages.help.player", "§e/expbooster player <玩家名> <倍率> <时长> [选项] §7- §f设置玩家经验加成"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.player_options", "§7  选项: §f-levelGroup=<组名> -source=<来源> -silent"));
        }
        
        if (sender.hasPermission("expboostqwq.command.server")) {
            sender.sendMessage(lang.getMessage(langCode, "messages.help.server", "§e/expbooster server <倍率> <时长> [选项] §7- §f设置全服经验加成"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.server_options", "§7  选项: §f-levelGroup=<组名> -source=<来源> -silent"));
        }
        
        if (sender.hasPermission("expboostqwq.command.check")) {
            sender.sendMessage(lang.getMessage(langCode, "messages.help.check_player", "§e/expbooster check player [玩家名] §7- §f检查玩家经验加成状态"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.check_server", "§e/expbooster check server §7- §f检查全服经验加成状态"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.check_global", "§e/expbooster check global §7- §f检查全局默认倍率"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.check_groups", "§e/expbooster check groups §7- §f检查所有等级组倍率"));
        }
        
        if (sender.hasPermission("expboostqwq.admin")) {
            sender.sendMessage(lang.getMessage(langCode, "messages.help.global", "§e/expbooster global <倍率> §7- §f设置全局默认倍率"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.group", "§e/expbooster group <等级组> <倍率> §7- §f设置等级组倍率"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.groups", "§e/expbooster groups §7- §f列出所有等级组及倍率"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.reload", "§e/expbooster reload §7- §f重载插件配置"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.logs", "§e/expbooster logs [行数/日期] [行数] §7- §f查看日志"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.logs_list", "§e/expbooster logs list §7- §f列出所有日志文件"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.disable_player", "§e/expbooster disable player <玩家名> §7- §f关闭玩家经验加成"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.disable_server", "§e/expbooster disable server §7- §f关闭全服经验加成"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.disable_group", "§e/expbooster disable group <等级组> §7- §f关闭等级组经验加成"));
            
            // 加成计算方式提示
            String calculationType = plugin.getConfig().getString("settings.boost_calculation", "multiply");
            String calcTypeName;
            if (calculationType.equalsIgnoreCase("highest")) {
                calcTypeName = "取最高倍率";
            } else if (calculationType.equalsIgnoreCase("add")) {
                calcTypeName = "相加模式";
            } else {
                calcTypeName = "相乘模式";
            }
            sender.sendMessage(lang.getMessage(langCode, "messages.help.boost_calculation", 
                    "§7[配置信息] 当前加成计算方式: §e%mode% §7(可在config.yml中修改)")
                    .replace("%mode%", calcTypeName));
        }
        
        // 语言命令帮助
        sender.sendMessage(lang.getMessage(langCode, "messages.help.language", "§e/expbooster language [语言代码] §7- §f设置或查看你的语言"));
        
        if (sender.hasPermission("expboostqwq.admin")) {
            sender.sendMessage(lang.getMessage(langCode, "messages.help.language_player", "§e/expbooster language player <玩家名> <语言代码> §7- §f设置玩家的语言"));
            sender.sendMessage(lang.getMessage(langCode, "messages.help.language_server", "§e/expbooster language server <语言代码> §7- §f设置服务器默认语言"));
        }
        
        sender.sendMessage(lang.getMessage(langCode, "messages.help.language_list", "§e/expbooster language list §7- §f列出所有可用的语言"));
        sender.sendMessage(lang.getMessage(langCode, "messages.help.footer", "§6==========================="));
    }
    
    /**
     * 处理语言设置命令
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleLanguage(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 显示当前语言
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String langCode = plugin.getLanguageManager().getPlayerLanguage(player.getUniqueId().toString());
                String langName = plugin.getLanguageManager().getLanguage(langCode).getString("language.name", langCode);
                
                String message = plugin.getLanguageManager().getMessage(
                        langCode, 
                        "messages.command.language_current", 
                        "§a当前语言: §e%language%");
                sender.sendMessage(message.replace("%language%", langName));
            } else {
                String langCode = plugin.getLanguageManager().getDefaultLanguage();
                String langName = plugin.getLanguageManager().getLanguage(langCode).getString("language.name", langCode);
                
                String message = plugin.getLanguageManager().getMessage(
                        langCode, 
                        "messages.command.language_current", 
                        "§a当前语言: §e%language%");
                sender.sendMessage(message.replace("%language%", langName));
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode, 
                        "messages.usage.language", 
                        "§c用法: /expbooster language [语言代码]"));
            }
            return;
        }
        
        String action = args[1].toLowerCase();
        
        if (action.equals("list")) {
            // 列出所有可用的语言
            Map<String, String> languages = plugin.getLanguageManager().getAvailableLanguages();
            
            if (languages.isEmpty()) {
                sender.sendMessage("§c没有找到任何语言文件");
                return;
            }
            
            String langsList = languages.entrySet().stream()
                    .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                    .collect(Collectors.joining(", "));
            
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    "messages.command.available_languages", 
                    "§a可用的语言: %languages%")
                    .replace("%languages%", langsList));
            return;
        } else if (action.equals("player")) {
            // 设置玩家的语言
            if (!sender.hasPermission("expboostqwq.admin")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        "messages.command.no_permission", 
                        "§c你没有权限执行此命令!"));
                return;
            }
            
            if (args.length < 4) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        "messages.usage.language_player", 
                        "§c用法: /expbooster language player <玩家名> <语言代码>"));
                return;
            }
            
            String playerName = args[2];
            Player target = Bukkit.getPlayerExact(playerName);
            
            if (target == null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        "messages.command.player_not_found", 
                        "§c找不到玩家: %player%")
                        .replace("%player%", playerName));
                return;
            }
            
            String langCode = args[3];
            Map<String, String> languages = plugin.getLanguageManager().getAvailableLanguages();
            
            if (!languages.containsKey(langCode)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        "messages.command.language_not_found", 
                        "§c找不到语言: %language%")
                        .replace("%language%", langCode));
                
                if (!languages.isEmpty()) {
                    String langsList = String.join(", ", languages.keySet());
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            "messages.command.available_languages", 
                            "§a可用的语言: %languages%")
                            .replace("%languages%", langsList));
                }
                return;
            }
            
            // 设置玩家语言
            plugin.getLanguageManager().setPlayerLanguage(target.getUniqueId().toString(), langCode);
            
            // 向管理员发送确认消息
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    "messages.command.language_set_other", 
                    "§a已将玩家 §e%player% §a的语言设置为: §e%language%")
                    .replace("%player%", target.getName())
                    .replace("%language%", languages.get(langCode)));
            
            // 向玩家发送通知
            if (sender != target) {
                target.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.command.language_set", 
                        "§a你的语言已设置为: §e%language%")
                        .replace("%language%", languages.get(langCode)));
            }
            
            return;
        } else if (action.equals("server")) {
            // 设置服务器默认语言
            if (!sender.hasPermission("expboostqwq.admin")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        "messages.command.no_permission", 
                        "§c你没有权限执行此命令!"));
                return;
            }
            
            if (args.length < 3) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        "messages.usage.language_server", 
                        "§c用法: /expbooster language server <语言代码>"));
                return;
            }
            
            String langCode = args[2];
            Map<String, String> languages = plugin.getLanguageManager().getAvailableLanguages();
            
            if (!languages.containsKey(langCode)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        "messages.command.language_not_found", 
                        "§c找不到语言: %language%")
                        .replace("%language%", langCode));
                
                if (!languages.isEmpty()) {
                    String langsList = String.join(", ", languages.keySet());
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            "messages.command.available_languages", 
                            "§a可用的语言: %languages%")
                            .replace("%languages%", langsList));
                }
                return;
            }
            
            // 设置服务器默认语言
            plugin.getLanguageManager().setDefaultLanguage(langCode);
            
            // 向管理员发送确认消息
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    "messages.command.server_language_set", 
                    "§a服务器默认语言已设置为: §e%language%")
                    .replace("%language%", languages.get(langCode)));
            
            return;
        } else {
            // 设置玩家自己的语言
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        "messages.usage.language", 
                        "§c用法: /expbooster language [语言代码]"));
                return;
            }
            
            Player player = (Player) sender;
            String langCode = args[1];
            Map<String, String> languages = plugin.getLanguageManager().getAvailableLanguages();
            
            if (!languages.containsKey(langCode)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        "messages.command.language_not_found", 
                        "§c找不到语言: %language%")
                        .replace("%language%", langCode));
                
                if (!languages.isEmpty()) {
                    String langsList = String.join(", ", languages.keySet());
                    sender.sendMessage(plugin.getLanguageManager().getMessage(
                            "messages.command.available_languages", 
                            "§a可用的语言: %languages%")
                            .replace("%languages%", langsList));
                }
                return;
            }
            
            // 设置玩家语言
            plugin.getLanguageManager().setPlayerLanguage(player.getUniqueId().toString(), langCode);
            
            // 发送确认消息
            player.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.language_set", 
                    "§a你的语言已设置为: §e%language%")
                    .replace("%language%", languages.get(langCode)));
        }
    }
    
    /**
     * 处理关闭经验加成命令
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleDisable(CommandSender sender, String[] args) {
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
        
        if (!sender.hasPermission("expboostqwq.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.no_permission",
                    "§c[ExpboostQwQ] 你没有权限执行此命令!"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.usage.disable",
                    "§c[ExpboostQwQ] 用法: /expbooster disable <player|server|group> [名称]"));
            return;
        }
        
        String type = args[1].toLowerCase();
        
        switch (type) {
            case "player":
                handleDisablePlayer(sender, args, langCode);
                break;
            case "server":
                handleDisableServer(sender, langCode);
                break;
            case "group":
                handleDisableGroup(sender, args, langCode);
                break;
            default:
                sender.sendMessage(plugin.getLanguageManager().getMessage(
                        langCode,
                        "messages.usage.disable",
                        "§c[ExpboostQwQ] 用法: /expbooster disable <player|server|group> [名称]"));
                break;
        }
    }
    
    /**
     * 处理关闭玩家经验加成
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     * @param langCode 语言代码
     */
    private void handleDisablePlayer(CommandSender sender, String[] args, String langCode) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.usage.disable_player",
                    "§c[ExpboostQwQ] 用法: /expbooster disable player <玩家名>"));
            return;
        }
        
        String playerName = args[2];
        Player target = Bukkit.getPlayerExact(playerName);
        
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.player_not_found",
                    "§c[ExpboostQwQ] 找不到玩家: %player%")
                    .replace("%player%", playerName));
            return;
        }
        
        UUID targetUUID = target.getUniqueId();
        
        // 检查玩家是否有加成
        if (!plugin.getBoosterManager().hasPlayerBooster(targetUUID)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.no_player_booster",
                    "§a[ExpboostQwQ] 玩家 §e%player% §a当前没有个人经验加成")
                    .replace("%player%", target.getName()));
            return;
        }
        
        // 记录旧的倍率用于日志
        double oldMultiplier = plugin.getBoosterManager().getPlayerBooster(targetUUID).getMultiplier();
        
        // 移除玩家加成
        plugin.getBoosterManager().removePlayerBooster(targetUUID);
        
        // 记录倍率变化
        LogUtil.multiplierChange(target, oldMultiplier, 1.0, 0, "个人");
        
        // 发送确认消息
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.exp_boost.player_booster_removed",
                "§c[ExpboostQwQ] 已移除玩家 §e%player% §c的经验加成")
                .replace("%player%", target.getName()));
        
        // 通知玩家
        if (!sender.equals(target)) {
            target.sendMessage(plugin.getLanguageManager().getMessage(
                    plugin.getLanguageManager().getPlayerLanguage(target.getUniqueId().toString()),
                    "messages.exp_boost.player_booster_removed_self",
                    "§c[ExpboostQwQ] 你的经验加成已被移除"));
        }
        
        // 记录日志
        LogUtil.debug("管理员 " + sender.getName() + " 移除了玩家 " + target.getName() + " 的经验加成");
    }
    
    /**
     * 处理关闭服务器经验加成
     * 
     * @param sender 命令发送者
     * @param langCode 语言代码
     */
    private void handleDisableServer(CommandSender sender, String langCode) {
        ServerBooster serverBooster = plugin.getBoosterManager().getServerBooster();
        
        // 检查是否有全服加成
        if (serverBooster == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.no_server_booster",
                    "§a[ExpboostQwQ] 当前没有全服经验加成"));
            return;
        }
        
        // 记录旧的倍率用于日志
        double oldMultiplier = serverBooster.getMultiplier();
        
        // 移除全服加成
        plugin.getBoosterManager().removeServerBooster();
        
        // 记录倍率变化
        LogUtil.multiplierChange(null, oldMultiplier, 1.0, 0, "全服");
        
        // 发送确认消息
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.exp_boost.server_booster_removed",
                "§c[ExpboostQwQ] 已移除全服经验加成"));
        
        // 广播消息
        Bukkit.broadcastMessage(plugin.getLanguageManager().getMessage(
                plugin.getLanguageManager().getDefaultLanguage(),
                "messages.exp_boost.server_booster_removed_broadcast",
                "§c[ExpboostQwQ] 全服经验加成已被移除"));
        
        // 记录日志
        LogUtil.debug("管理员 " + sender.getName() + " 移除了全服经验加成");
    }
    
    /**
     * 处理关闭等级组经验加成
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     * @param langCode 语言代码
     */
    private void handleDisableGroup(CommandSender sender, String[] args, String langCode) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.usage.disable_group",
                    "§c[ExpboostQwQ] 用法: /expbooster disable group <等级组>"));
            return;
        }
        
        String groupName = args[2];
        
        // 检查等级组是否存在
        if (!LevelApiUtil.isLevelGroupExists(groupName)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.level_group_not_exists",
                    "§c[ExpboostQwQ] 错误: 等级组 '%group%' 不存在")
                    .replace("%group%", groupName));
            
            String availableGroups = String.join(", ", LevelApiUtil.getLevelGroupNames());
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.available_level_groups",
                    "§c[ExpboostQwQ] 可用的等级组: %groups%")
                    .replace("%groups%", availableGroups));
            return;
        }
        
        // 检查等级组是否有加成
        if (!plugin.getBoosterManager().hasLevelGroupBooster(groupName)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.no_group_booster",
                    "§a[ExpboostQwQ] 等级组 §e%group% §a当前没有特定经验加成")
                    .replace("%group%", groupName));
            return;
        }
        
        // 记录旧的倍率用于日志
        double oldMultiplier = plugin.getBoosterManager().getLevelGroupMultiplier(groupName);
        
        // 移除等级组加成
        plugin.getBoosterManager().removeLevelGroupBooster(groupName);
        
        // 记录倍率变化
        LogUtil.multiplierChange(null, oldMultiplier, 1.0, 0, "等级组 " + groupName);
        
        // 发送确认消息
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.exp_boost.group_booster_removed",
                "§c[ExpboostQwQ] 已移除等级组 §e%group% §c的经验加成")
                .replace("%group%", groupName));
        
        // 广播消息
        Bukkit.broadcastMessage(plugin.getLanguageManager().getMessage(
                plugin.getLanguageManager().getDefaultLanguage(),
                "messages.exp_boost.group_booster_removed_broadcast",
                "§c[ExpboostQwQ] 等级组 §e%group% §c的经验加成已被移除")
                .replace("%group%", groupName));
        
        // 记录日志
        LogUtil.debug("管理员 " + sender.getName() + " 移除了等级组 " + groupName + " 的经验加成");
    }
    
    /**
     * 命令补全处理
     * 
     * @param command 命令对象
     * @param alias 命令别名
     * @param args 当前参数
     * @return 可能的补全列表
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 主要子命令补全
            if (sender.hasPermission("expboostqwq.command.help")) {
                completions.add("help");
            }
            
            if (sender.hasPermission("expboostqwq.command.check")) {
                completions.add("check");
            }
            
            if (sender.hasPermission("expboostqwq.command.player")) {
                completions.add("player");
            }
            
            if (sender.hasPermission("expboostqwq.command.server")) {
                completions.add("server");
            }
            
            if (sender.hasPermission("expboostqwq.admin")) {
                completions.add("global");
                completions.add("group");
                completions.add("groups");
                completions.add("reload");
                completions.add("logs");
                completions.add("language");
                completions.add("disable");
                completions.add("checkupdate");
            }
            
            return filterCompletions(completions, args[0]);
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("player") && sender.hasPermission("expboostqwq.command.player")) {
            if (args.length == 2) {
                // 补全玩家名
                return null; // 返回null将使用服务器默认的玩家名补全
            } else if (args.length == 3) {
                // 补全倍率
                completions.add("1.5");
                completions.add("2.0");
                completions.add("3.0");
                return filterCompletions(completions, args[2]);
            } else if (args.length == 4) {
                // 补全时长
                completions.add("1h");
                completions.add("1d");
                completions.add("7d");
                completions.add("30d");
                completions.add("permanent");
                return filterCompletions(completions, args[3]);
            } else if (args.length >= 5) {
                // 补全选项
                if (!args[args.length - 1].startsWith("-")) {
                    completions.add("-levelGroup=");
                    completions.add("-source=");
                    completions.add("-silent");
                    return filterCompletions(completions, args[args.length - 1]);
                } else if (args[args.length - 1].startsWith("-levelGroup=")) {
                    // 补全等级组
                    String prefix = "-levelGroup=";
                    String current = args[args.length - 1].substring(prefix.length());
                    List<String> groups = LevelApiUtil.getLevelGroupNames();
                    return groups.stream()
                            .filter(group -> group.toLowerCase().startsWith(current.toLowerCase()))
                            .map(group -> prefix + group)
                            .collect(Collectors.toList());
                }
            }
        } else if (subCommand.equals("server") && sender.hasPermission("expboostqwq.command.server")) {
            if (args.length == 2) {
                // 补全倍率
                completions.add("1.5");
                completions.add("2.0");
                completions.add("3.0");
                return filterCompletions(completions, args[1]);
            } else if (args.length == 3) {
                // 补全时长
                completions.add("1h");
                completions.add("1d");
                completions.add("7d");
                completions.add("30d");
                completions.add("permanent");
                return filterCompletions(completions, args[2]);
            } else if (args.length >= 4) {
                // 补全选项
                if (!args[args.length - 1].startsWith("-")) {
                    completions.add("-levelGroup=");
                    completions.add("-source=");
                    completions.add("-silent");
                    return filterCompletions(completions, args[args.length - 1]);
                } else if (args[args.length - 1].startsWith("-levelGroup=")) {
                    // 补全等级组
                    String prefix = "-levelGroup=";
                    String current = args[args.length - 1].substring(prefix.length());
                    List<String> groups = LevelApiUtil.getLevelGroupNames();
                    return groups.stream()
                            .filter(group -> group.toLowerCase().startsWith(current.toLowerCase()))
                            .map(group -> prefix + group)
                            .collect(Collectors.toList());
                }
            }
        } else if (subCommand.equals("check") && sender.hasPermission("expboostqwq.command.check")) {
            if (args.length == 2) {
                // 补全检查类型
                completions.add("player");
                completions.add("server");
                completions.add("global");
                completions.add("groups");
                return filterCompletions(completions, args[1]);
            } else if (args.length == 3 && args[1].equalsIgnoreCase("player")) {
                // 补全玩家名
                return null; // 返回null将使用服务器默认的玩家名补全
            }
        } else if (subCommand.equals("global") && sender.hasPermission("expboostqwq.admin")) {
            if (args.length == 2) {
                // 补全倍率
                completions.add("1.0");
                completions.add("1.5");
                completions.add("2.0");
                return filterCompletions(completions, args[1]);
            } else if (args.length == 3) {
                // 补全时长
                completions.add("1h");
                completions.add("1d");
                completions.add("7d");
                completions.add("30d");
                completions.add("permanent");
                return filterCompletions(completions, args[2]);
            } else if (args.length >= 4) {
                // 补全选项
                if (!args[args.length - 1].startsWith("-")) {
                    completions.add("-levelGroup=");
                    completions.add("-source=");
                    completions.add("-silent");
                    return filterCompletions(completions, args[args.length - 1]);
                } else if (args[args.length - 1].startsWith("-levelGroup=")) {
                    // 补全等级组
                    String prefix = "-levelGroup=";
                    String current = args[args.length - 1].substring(prefix.length());
                    List<String> groups = LevelApiUtil.getLevelGroupNames();
                    return groups.stream()
                            .filter(group -> group.toLowerCase().startsWith(current.toLowerCase()))
                            .map(group -> prefix + group)
                            .collect(Collectors.toList());
                }
            }
        } else if (subCommand.equals("group") && sender.hasPermission("expboostqwq.admin")) {
            if (args.length == 2) {
                // 补全等级组
                return filterCompletions(LevelApiUtil.getLevelGroupNames(), args[1]);
            } else if (args.length == 3) {
                // 补全倍率
                completions.add("1.0");
                completions.add("1.5");
                completions.add("2.0");
                return filterCompletions(completions, args[2]);
            } else if (args.length == 4) {
                // 补全时长
                completions.add("1h");
                completions.add("1d");
                completions.add("7d");
                completions.add("30d");
                completions.add("permanent");
                return filterCompletions(completions, args[3]);
            } else if (args.length >= 5) {
                // 补全选项
                if (!args[args.length - 1].startsWith("-")) {
                    completions.add("-levelGroup=");
                    completions.add("-source=");
                    completions.add("-silent");
                    return filterCompletions(completions, args[args.length - 1]);
                } else if (args[args.length - 1].startsWith("-levelGroup=")) {
                    // 补全等级组
                    String prefix = "-levelGroup=";
                    String current = args[args.length - 1].substring(prefix.length());
                    List<String> groups = LevelApiUtil.getLevelGroupNames();
                    return groups.stream()
                            .filter(group -> group.toLowerCase().startsWith(current.toLowerCase()))
                            .map(group -> prefix + group)
                            .collect(Collectors.toList());
                }
            }
        } else if (subCommand.equals("disable") && sender.hasPermission("expboostqwq.admin")) {
            if (args.length == 2) {
                // 补全disable类型
                completions.add("player");
                completions.add("server");
                completions.add("group");
                return filterCompletions(completions, args[1]);
            } else if (args.length == 3) {
                if (args[1].equalsIgnoreCase("player")) {
                    // 补全玩家名
                    return null; // 返回null使用服务器默认玩家名补全
                } else if (args[1].equalsIgnoreCase("group")) {
                    // 补全等级组名
                    return filterCompletions(LevelApiUtil.getLevelGroupNames(), args[2]);
                }
            }
        }
        
        return completions;
    }
    
    /**
     * 过滤补全选项，只保留以用户输入开头的选项
     * 
     * @param completions 补全选项列表
     * @param current 当前用户输入
     * @return 过滤后的补全选项
     */
    private List<String> filterCompletions(List<String> completions, String current) {
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(current.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    /**
     * 处理检查更新子命令
     * 
     * @param sender 命令发送者
     */
    private void handleCheckUpdate(CommandSender sender) {
        // 获取玩家语言，如果是控制台则使用默认语言
        String langCode = sender instanceof Player 
                ? plugin.getLanguageManager().getPlayerLanguage(((Player) sender).getUniqueId().toString())
                : plugin.getLanguageManager().getDefaultLanguage();
        
        if (!sender.hasPermission("expboostqwq.command.checkupdate")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.no_permission",
                    "§c[ExpboostQwQ] 你没有权限执行此命令!"));
            return;
        }
        
        sender.sendMessage(plugin.getLanguageManager().getMessage(
                langCode,
                "messages.command.checking_update",
                "§b[ExpboostQwQ] §f正在检查更新..."));
        
        // 检查是否启用了更新检查
        if (!plugin.getConfig().getBoolean("settings.check_update", true)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.update_checking_disabled",
                    "§c[ExpboostQwQ] 更新检查功能已在配置中禁用！"));
            return;
        }
        
        // 如果更新检查器为空，则创建一个
        if (plugin.getUpdateChecker() == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.update_checker_initializing",
                    "§b[ExpboostQwQ] §f更新检查器尚未初始化，正在创建并检查更新..."));
            UpdateChecker updateChecker = new UpdateChecker(plugin);
            updateChecker.checkForUpdates();
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.update_check_started",
                    "§b[ExpboostQwQ] §f更新检查已启动，请稍后查看控制台获取结果。"));
            return;
        }
        
        // 检查是否已有更新
        if (plugin.getUpdateChecker().isUpdateAvailable()) {
            String message = plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.update_available",
                    "§b[ExpboostQwQ] §f检测到新版本: §a%latest_version% §f(当前版本: §c%current_version%§f)");
            message = message.replace("%latest_version%", plugin.getUpdateChecker().getLatestVersion())
                            .replace("%current_version%", plugin.getDescription().getVersion());
            sender.sendMessage(message);
            
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.update_download_link",
                    "§b[ExpboostQwQ] §f请前往 §ehttps://github.com/znc15/ExpboostQwQ/releases §f下载最新版本"));
        } else {
            // 重新检查更新
            plugin.getUpdateChecker().checkForUpdates();
            sender.sendMessage(plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.command.rechecking_update",
                    "§b[ExpboostQwQ] §f正在重新检查更新，结果将在几秒钟后显示在控制台中。"));
        }
    }
} 
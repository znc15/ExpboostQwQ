package org.littlesheep.expboostQwQ;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.littlesheep.expboostQwQ.commands.ExpBoosterCommand;
import org.littlesheep.expboostQwQ.data.BoosterManager;
import org.littlesheep.expboostQwQ.hooks.PlaceholderAPIHook;
import org.littlesheep.expboostQwQ.listeners.ExpGainListener;
import org.littlesheep.expboostQwQ.utils.LanguageManager;
import org.littlesheep.expboostQwQ.utils.LogUtil;
import org.littlesheep.expboostQwQ.utils.LevelApiUtil;

/**
 * ExpboostQwQ 插件主类
 * 
 * @author LittleSheep
 */
public final class ExpboostQwQ extends JavaPlugin {
    
    // 插件单例实例
    private static ExpboostQwQ instance;
    // 经验加成管理器
    private BoosterManager boosterManager;
    // 语言管理器
    private LanguageManager languageManager;
    private boolean bStatsEnabled;

    private void printLogo() {
        String[] logo = {
            "§b ______                ____                    _    ____        ____  ",
            "§b|  ____|              |  _ \\                  | |  / __ \\      / __ \\ ",
            "§b| |__  __  ___ __     | |_) | ___   ___  ___ | |_| |  | |_  _| |  | |",
            "§b|  __| \\ \\/ / '_ \\    |  _ < / _ \\ / _ \\/ __|| __| |  | \\ \\/ / |  | |",
            "§b| |____ >  <| |_) |   | |_) | (_) | (_) \\__ \\| |_| |__| |>  <| |__| |",
            "§b|______/_/\\_\\ .__/    |____/ \\___/ \\___/|___/ \\__\\\\___\\_\\/_/ \\___\\_\\",
            "§b            | |                                                        ",
            "§b            |_|                                                        ",
            "§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "§b作者: §fLittleSheep",
            "§b版本: §f" + getDescription().getVersion(),
            "§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        };
        
        for (String line : logo) {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    /**
     * 插件启用时执行
     * 初始化配置、管理器、命令和监听器
     */
    @Override
    public void onEnable() {
        // 保存实例引用
        instance = this;
        
        // 打印 LOGO
        printLogo();
        
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化日志工具类
        LogUtil.init(this);
        
        // 初始化语言管理器（在BoosterManager之前初始化）
        languageManager = new LanguageManager(this);
        
        // 初始化 LevelApiUtil
        LevelApiUtil.init(this);
        
        // 初始化Booster管理器
        boosterManager = new BoosterManager(this);
        
        // 注册命令
        registerCommands();
        
        // 注册监听器
        registerListeners();
        
        // 注册PAPI扩展
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
            LogUtil.info("§b[ExpboostQwQ] §fPlaceholderAPI扩展已注册");
        }
        
        // 初始化bStats
        bStatsEnabled = getConfig().getBoolean("settings.enable_bstats", true);
        if (bStatsEnabled) {
            int pluginId = 25432;
            new Metrics(this, pluginId);
            LogUtil.info("§b[ExpboostQwQ] §fbStats统计已启用");
        }
        
        // 显示启用消息
        String enableMessage = languageManager.getMessage("messages.plugin.enabled", "§a插件已启用！默认语言: %language%");
        enableMessage = enableMessage.replace("%language%", languageManager.getDefaultLanguage());
        LogUtil.info("§b[ExpboostQwQ] §f" + enableMessage);
        
        // 显示启动完成消息
        Bukkit.getConsoleSender().sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.getConsoleSender().sendMessage("§b[ExpboostQwQ] §f插件启动完成！");
        Bukkit.getConsoleSender().sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 插件禁用时执行
     * 保存所有数据
     */
    @Override
    public void onDisable() {
        // 显示禁用消息
        Bukkit.getConsoleSender().sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.getConsoleSender().sendMessage("§b[ExpboostQwQ] §f正在保存数据...");
        
        if (boosterManager != null) {
            boosterManager.saveAll();
        }
        
        Bukkit.getConsoleSender().sendMessage("§b[ExpboostQwQ] §f数据保存完成！");
        Bukkit.getConsoleSender().sendMessage("§b[ExpboostQwQ] §f插件已禁用！");
        Bukkit.getConsoleSender().sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    /**
     * 重载插件配置和数据
     */
    public void reload() {
        LogUtil.info("正在重载插件配置和数据...");
        
        // 重载配置文件
        reloadConfig();
        
        // 重新初始化日志工具
        LogUtil.init(this);
        
        // 重载语言文件
        if (languageManager != null) {
            languageManager.reload();
        }
        
        // 重载加成数据
        boosterManager.loadData();
        
        // 重新加载bStats设置
        boolean newBStatsEnabled = getConfig().getBoolean("settings.enable_bstats", true);
        if (newBStatsEnabled != bStatsEnabled) {
            bStatsEnabled = newBStatsEnabled;
            if (bStatsEnabled) {
                int pluginId = 25432;
                new Metrics(this, pluginId);
                LogUtil.info("bStats统计已启用");
            }
        }
        
        String reloadMessage = languageManager != null 
                ? languageManager.getMessage("messages.plugin.reload", "§a插件配置已重载！")
                : "§a插件配置已重载！";
        LogUtil.info(reloadMessage);
    }
    
    /**
     * 注册插件命令
     * 将命令执行器和Tab补全器绑定到命令
     */
    private void registerCommands() {
        PluginCommand command = getCommand("expbooster");
        if (command != null) {
            ExpBoosterCommand commandExecutor = new ExpBoosterCommand(this);
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }
    }
    
    /**
     * 注册事件监听器
     * 监听AkariLevel的经验变更事件
     */
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new ExpGainListener(this), this);
    }
    
    /**
     * 获取插件实例
     * 提供全局访问点
     * 
     * @return 插件实例
     */
    public static ExpboostQwQ getInstance() {
        return instance;
    }
    
    /**
     * 获取经验加成管理器
     * 
     * @return 经验加成管理器实例
     */
    public BoosterManager getBoosterManager() {
        return boosterManager;
    }
    
    /**
     * 获取语言管理器
     * 
     * @return 语言管理器实例
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
}

package org.littlesheep.expboostQwQ;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.littlesheep.expboostQwQ.commands.ExpBoosterCommand;
import org.littlesheep.expboostQwQ.data.BoosterManager;
import org.littlesheep.expboostQwQ.hooks.PlaceholderAPIHook;
import org.littlesheep.expboostQwQ.listeners.ExpGainListener;
import org.littlesheep.expboostQwQ.utils.LanguageManager;
import org.littlesheep.expboostQwQ.utils.LogUtil;
import org.littlesheep.expboostQwQ.utils.LevelApiUtil;
import org.littlesheep.expboostQwQ.utils.UpdateChecker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

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
    // 更新检查器
    private UpdateChecker updateChecker;

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
        
        // 保存默认配置文件
        saveDefaultConfig();
        
        // 初始化日志工具类
        LogUtil.init(this);
        
        // 初始化语言管理器
        languageManager = new LanguageManager(this);
        
        // 初始化 LevelApiUtil
        LevelApiUtil.init(this);
        
        // 初始化加成管理器
        boosterManager = new BoosterManager(this);
        
        // 注册命令
        getCommand("expbooster").setExecutor(new ExpBoosterCommand(this));
        getCommand("expbooster").setTabCompleter(new ExpBoosterCommand(this));
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new ExpGainListener(this), this);
        
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
        
        // 检查更新
        if (getConfig().getBoolean("settings.check_update", true)) {
            updateChecker = new UpdateChecker(this);
            updateChecker.checkForUpdates();
        }
        
        // 启动日志清理任务
        if (getConfig().getBoolean("settings.logs.auto_delete", true)) {
            int checkInterval = getConfig().getInt("settings.logs.check_interval", 60);
            
            getServer().getScheduler().runTaskTimer(this, () -> {
                LogUtil.cleanupLogs();
            }, 20L * 60 * checkInterval, 20L * 60 * checkInterval);
            
            if (getConfig().getBoolean("settings.logs.check_on_startup", true)) {
                LogUtil.cleanupLogs();
            }
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
        // 取消所有定时任务
        if (boosterManager != null) {
            boosterManager.cancelTasks();
            // 保存所有加成数据
            boosterManager.saveAll();
        }
        
        // 输出插件已禁用的消息
        getLogger().info("ExpboostQwQ v" + getDescription().getVersion() + " 已禁用!");
    }
    
    /**
     * 重载插件配置
     */
    public void reload() {
        // 使用YamlConfiguration从文件加载配置而不是使用reloadConfig()
        File configFile = new File(getDataFolder(), "config.yml");
        if (configFile.exists()) {
            try {
                FileConfiguration newConfig = YamlConfiguration.loadConfiguration(configFile);
                for (String key : newConfig.getKeys(true)) {
                    getConfig().set(key, newConfig.get(key));
                }
                LogUtil.debug("从文件重新加载了配置");
            } catch (Exception e) {
                LogUtil.error("加载配置文件失败", e);
            }
        }
        
        languageManager.reload();
        boosterManager.loadData();
        LogUtil.info("插件配置已重载！");
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
    
    /**
     * 获取更新检查器
     * 
     * @return 更新检查器实例
     */
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
    
    /**
     * 保存配置文件（保留注释）
     * 使用标准的saveConfig方法，同时记录日志
     */
    public void saveConfigWithComments() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            
            // 如果文件不存在，先创建一个默认配置
            if (!configFile.exists()) {
                saveDefaultConfig();
                return;
            }
            
            // 用 YamlConfiguration 重新加载文件以保留注释
            YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(configFile);
            
            // 将内存中配置的值复制到新的 YamlConfiguration 中
            for (String key : getConfig().getKeys(true)) {
                // 只更新值，不改变注释和结构
                if (getConfig().isSet(key)) {
                    yamlConfig.set(key, getConfig().get(key));
                }
            }
            
            // 保存文件
            yamlConfig.save(configFile);
            LogUtil.debug("已保存配置文件（保留注释）");
        } catch (IOException e) {
            LogUtil.error("保存配置文件时出错", e);
            // 如果出错，回退到标准方法
            saveConfig();
        }
    }
}

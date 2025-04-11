package org.littlesheep.expboostQwQ.utils;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Player;
import org.littlesheep.expboostQwQ.ExpboostQwQ;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 日志工具类
 * 统一管理插件的日志输出，支持不同级别的日志和格式化
 */
public class LogUtil {
    private static Logger logger;
    private static boolean debugMode = false;
    private static File logFile;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final int MAX_LOG_LINES = 1000; // 最大日志行数限制
    private static ExpboostQwQ plugin;
    private static BukkitRunnable cleanupTask;
    
    /**
     * 初始化日志工具类
     * @param plugin 插件主类实例
     */
    public static void init(ExpboostQwQ pluginInstance) {
        plugin = pluginInstance;
        logger = plugin.getLogger();
        debugMode = plugin.getConfig().getBoolean("settings.debug_mode", false);
        
        // 创建日志文件夹
        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        
        // 创建今天的日志文件
        String fileName = fileNameFormat.format(new Date()) + ".log";
        logFile = new File(logsDir, fileName);
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (IOException e) {
            logger.severe("无法创建日志文件: " + e.getMessage());
        }
        
        // 如果配置了启动时检查，执行一次清理
        if (plugin.getConfig().getBoolean("settings.logs.check_on_startup", true)) {
            cleanupOldLogs();
        }
        
        // 如果启用了自动删除，启动定时任务
        if (plugin.getConfig().getBoolean("settings.logs.auto_delete", true)) {
            startCleanupTask();
        }
    }
    
    /**
     * 启动自动清理任务
     */
    private static void startCleanupTask() {
        // 如果已有任务在运行，先停止它
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        
        // 获取检查间隔（分钟）
        long interval = plugin.getConfig().getLong("settings.logs.check_interval", 60);
        
        // 创建新的清理任务
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOldLogs();
            }
        };
        
        // 启动任务（将分钟转换为tick）
        cleanupTask.runTaskTimer(plugin, interval * 1200L, interval * 1200L);
    }
    
    /**
     * 清理旧日志文件
     */
    public static void cleanupOldLogs() {
        if (!plugin.getConfig().getBoolean("settings.logs.auto_delete", true)) {
            return;
        }
        
        File logsDir = logFile.getParentFile();
        if (logsDir == null || !logsDir.exists()) {
            return;
        }
        
        // 获取保留天数
        int keepDays = plugin.getConfig().getInt("settings.logs.keep_days", 30);
        long cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(keepDays);
        
        File[] logFiles = logsDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (logFiles == null) {
            return;
        }
        
        int deletedCount = 0;
        for (File file : logFiles) {
            try {
                // 从文件名解析日期（格式：yyyy-MM-dd.log）
                String dateStr = file.getName().substring(0, 10);
                Date fileDate = fileNameFormat.parse(dateStr);
                
                // 如果文件日期早于截止日期且不是当前日志文件，删除它
                if (fileDate.getTime() < cutoffTime && !file.equals(logFile)) {
                    if (file.delete()) {
                        deletedCount++;
                        debug("已删除旧日志文件: " + file.getName());
                    }
                }
            } catch (ParseException e) {
                warn("无法解析日志文件日期: " + file.getName());
            }
        }
        
        if (deletedCount > 0) {
            info("自动清理完成，共删除 " + deletedCount + " 个旧日志文件");
        }
    }
    
    /**
     * 停止自动清理任务
     */
    public static void stopCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }
    
    /**
     * 输出信息级别日志
     * @param message 日志内容
     */
    public static void info(String message) {
        logger.info(message);
        writeToFile("[INFO] " + message);
    }
    
    /**
     * 输出警告级别日志
     * @param message 日志内容
     */
    public static void warn(String message) {
        logger.warning(message);
        writeToFile("[WARN] " + message);
    }
    
    /**
     * 输出错误级别日志
     * @param message 日志内容
     */
    public static void error(String message) {
        logger.severe(message);
        writeToFile("[ERROR] " + message);
    }
    
    /**
     * 输出错误级别日志，包含异常信息
     * @param message 日志内容
     * @param throwable 异常
     */
    public static void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
        writeToFile("[ERROR] " + message + "\n" + throwable.toString());
    }
    
    /**
     * 输出调试级别日志，仅在调试模式下输出
     * @param message 日志内容
     */
    public static void debug(String message) {
        if (debugMode) {
            logger.info("[Debug] " + message);
            writeToFile("[DEBUG] " + message);
        }
    }
    
    /**
     * 记录玩家经验加成相关日志
     * @param player 玩家对象
     * @param message 日志内容
     */
    public static void playerBooster(Player player, String message) {
        String logMessage = "[玩家加成] " + player.getName() + " (" + player.getUniqueId() + "): " + message;
        logger.info(logMessage);
        writeToFile(logMessage);
    }
    
    /**
     * 记录服务器经验加成相关日志
     * @param message 日志内容
     */
    public static void serverBooster(String message) {
        String logMessage = "[全服加成] " + message;
        logger.info(logMessage);
        writeToFile(logMessage);
    }
    
    /**
     * 记录玩家获得经验的详细信息
     * @param player 玩家对象
     * @param baseExp 基础经验值
     * @param finalExp 最终经验值
     * @param multiplier 倍率
     * @param source 经验来源
     * @param levelGroup 等级组
     */
    public static void expGain(Player player, double baseExp, double finalExp, double multiplier, String source, String levelGroup) {
        String logMessage = String.format("[经验获得] 玩家: %s, UUID: %s, 基础经验: %.2f, 最终经验: %.2f, 倍率: %.2f, 来源: %s, 等级组: %s",
                player.getName(), player.getUniqueId(), baseExp, finalExp, multiplier,
                source.isEmpty() ? "默认" : source,
                levelGroup.isEmpty() ? "默认" : levelGroup);
        debug(logMessage);
        writeToFile(logMessage);
    }
    
    /**
     * 记录命令执行日志
     * @param sender 命令执行者
     * @param command 完整命令
     * @param success 是否执行成功
     */
    public static void commandExecution(String sender, String command, boolean success) {
        String logMessage = String.format("[命令执行] 执行者: %s, 命令: %s, 状态: %s",
                sender, command, success ? "成功" : "失败");
        debug(logMessage);
        writeToFile(logMessage);
    }
    
    /**
     * 记录倍率变化
     * @param player 玩家对象（如果是针对特定玩家）
     * @param oldMultiplier 旧倍率
     * @param newMultiplier 新倍率
     * @param duration 持续时间（秒）
     * @param type 类型（个人/全服/等级组）
     */
    public static void multiplierChange(Player player, double oldMultiplier, double newMultiplier, long duration, String type) {
        String logMessage = String.format("[倍率变化] 类型: %s, %s, 旧倍率: %.2f, 新倍率: %.2f, 持续时间: %s",
                type,
                player != null ? "玩家: " + player.getName() : "全服",
                oldMultiplier,
                newMultiplier,
                duration == -1 ? "永久" : duration + "秒");
        debug(logMessage);
        writeToFile(logMessage);
    }
    
    /**
     * 将日志写入文件
     * @param message 日志消息
     */
    private static synchronized void writeToFile(String message) {
        if (logFile != null) {
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(dateFormat.format(new Date()) + " " + message + "\n");
            } catch (IOException e) {
                logger.severe("写入日志文件失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 设置是否启用调试模式
     * @param enabled 是否启用
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        logger.info("调试模式: " + (enabled ? "已启用" : "已禁用"));
    }
    
    /**
     * 读取最近的日志
     * @param lines 要读取的行数
     * @return 日志内容
     */
    public static String[] getRecentLogs(int lines) {
        if (logFile == null || !logFile.exists()) {
            return new String[]{"没有找到日志文件"};
        }
        
        // 限制最大行数
        lines = Math.min(lines, MAX_LOG_LINES);
        
        try {
            java.util.List<String> allLines = new java.util.ArrayList<>(
                java.nio.file.Files.readAllLines(logFile.toPath())
            );
            
            // 如果请求的行数大于实际行数，返回所有行
            if (lines >= allLines.size()) {
                return allLines.toArray(new String[0]);
            }
            
            // 否则返回最后的n行
            return allLines.subList(allLines.size() - lines, allLines.size())
                         .toArray(new String[0]);
        } catch (IOException e) {
            return new String[]{"读取日志文件失败: " + e.getMessage()};
        }
    }
    
    /**
     * 获取可用的日志文件列表
     * @return 日志文件列表
     */
    public static File[] getLogFiles() {
        File logsDir = logFile.getParentFile();
        if (logsDir != null && logsDir.exists()) {
            return logsDir.listFiles((dir, name) -> name.endsWith(".log"));
        }
        return new File[0];
    }
    
    /**
     * 从指定日期的日志文件中读取日志
     * @param date 日期（yyyy-MM-dd格式）
     * @param lines 要读取的行数
     * @return 日志内容
     */
    public static String[] getLogsFromDate(String date, int lines) {
        File logsDir = logFile.getParentFile();
        if (logsDir == null || !logsDir.exists()) {
            return new String[]{"日志目录不存在"};
        }
        
        File targetLog = new File(logsDir, date + ".log");
        if (!targetLog.exists()) {
            return new String[]{"找不到指定日期的日志文件"};
        }
        
        lines = Math.min(lines, MAX_LOG_LINES);
        
        try {
            java.util.List<String> allLines = new java.util.ArrayList<>(
                java.nio.file.Files.readAllLines(targetLog.toPath())
            );
            
            if (lines >= allLines.size()) {
                return allLines.toArray(new String[0]);
            }
            
            return allLines.subList(allLines.size() - lines, allLines.size())
                         .toArray(new String[0]);
        } catch (IOException e) {
            return new String[]{"读取日志文件失败: " + e.getMessage()};
        }
    }
    
    /**
     * 手动触发日志清理
     * @return 删除的文件数量
     */
    public static int forceCleanup() {
        File logsDir = logFile.getParentFile();
        if (logsDir == null || !logsDir.exists()) {
            return 0;
        }
        
        int keepDays = plugin.getConfig().getInt("settings.logs.keep_days", 30);
        long cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(keepDays);
        
        File[] logFiles = logsDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (logFiles == null) {
            return 0;
        }
        
        int deletedCount = 0;
        for (File file : logFiles) {
            try {
                String dateStr = file.getName().substring(0, 10);
                Date fileDate = fileNameFormat.parse(dateStr);
                
                if (fileDate.getTime() < cutoffTime && !file.equals(logFile)) {
                    if (file.delete()) {
                        deletedCount++;
                    }
                }
            } catch (ParseException e) {
                warn("无法解析日志文件日期: " + file.getName());
            }
        }
        
        return deletedCount;
    }
    
    /**
     * 清理过期的日志文件
     * @return 删除的文件数量
     */
    public static int cleanupLogs() {
        if (logFile == null || !logFile.getParentFile().exists()) {
            return 0;
        }

        File logDir = logFile.getParentFile();
        File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (logFiles == null) {
            return 0;
        }

        int deletedCount = 0;
        long keepDays = plugin.getConfig().getLong("settings.logs.keep_days", 30);
        long cutoffTime = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000);

        for (File file : logFiles) {
            if (file.equals(logFile)) {
                continue; // 跳过当前正在使用的日志文件
            }

            if (file.lastModified() < cutoffTime) {
                if (file.delete()) {
                    deletedCount++;
                    debug("已删除过期日志文件: " + file.getName());
                }
            }
        }

        return deletedCount;
    }
} 
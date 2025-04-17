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
import java.nio.charset.StandardCharsets;

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
            if (logsDir.mkdirs()) {
                debug("创建日志目录成功");
            } else {
                logger.severe("无法创建日志目录");
                return;
            }
        }
        
        // 创建今天的日志文件
        String fileName = fileNameFormat.format(new Date()) + ".log";
        logFile = new File(logsDir, fileName);
        try {
            if (!logFile.exists()) {
                if (logFile.createNewFile()) {
                    debug("创建日志文件成功: " + logFile.getAbsolutePath());
                    
                    // 写入初始内容确保文件不为空
                    try (FileWriter writer = new FileWriter(logFile)) {
                        writer.write(dateFormat.format(new Date()) + " [INFO] 日志系统初始化\n");
                    }
                } else {
                    logger.severe("无法创建日志文件，尽管调用了createNewFile方法");
                }
            } else {
                debug("使用现有日志文件: " + logFile.getAbsolutePath());
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
                String fileName = file.getName();
                // 确保文件名长度足够再进行截取
                if (fileName.length() < 10) {
                    warn("无效的日志文件名格式: " + fileName);
                    continue;
                }
                
                String dateStr = fileName.substring(0, 10);
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
            } catch (Exception e) {
                warn("处理日志文件时出错: " + file.getName() + ", 错误: " + e.getMessage());
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
        if (logFile == null) {
            logger.severe("日志文件对象为空，无法写入日志");
            return;
        }
        
        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile()) {
                    logger.severe("日志文件不存在且无法创建");
                    return;
                }
                logger.info("日志文件不存在，已重新创建: " + logFile.getAbsolutePath());
            } catch (IOException e) {
                logger.severe("创建日志文件失败: " + e.getMessage());
                return;
            }
        }
        
        if (!logFile.canWrite()) {
            logger.severe("日志文件不可写入: " + logFile.getAbsolutePath());
            return;
        }
        
        FileWriter writer = null;
        try {
            writer = new FileWriter(logFile, true);
            writer.write(dateFormat.format(new Date()) + " " + message + "\n");
            writer.flush();
        } catch (IOException e) {
            logger.severe("写入日志文件失败: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.severe("关闭日志文件失败: " + e.getMessage());
                }
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

        // 确保日志文件存在且可读
        if (!logFile.canRead()) {
            return new String[]{"无法读取日志文件：权限不足"};
        }

        // 限制最大行数
        lines = Math.min(lines, MAX_LOG_LINES);
        
        // 尝试不同的字符集读取文件
        java.util.List<String> allLines = new java.util.ArrayList<>();
        java.nio.charset.Charset[] charsets = {
            StandardCharsets.UTF_8,
            StandardCharsets.ISO_8859_1,
            java.nio.charset.Charset.forName("GBK"),
            java.nio.charset.Charset.forName("GB2312"),
            java.nio.charset.Charset.defaultCharset()
        };
        
        Exception lastException = null;
        boolean readSuccess = false;
        
        for (java.nio.charset.Charset charset : charsets) {
            try {
                debug("尝试使用字符集 " + charset.name() + " 读取当前日志文件");
                allLines = readLinesWithCharset(logFile, charset);
                readSuccess = true;
                debug("使用 " + charset.name() + " 成功读取当前日志文件");
                break;
            } catch (Exception e) {
                debug("使用 " + charset.name() + " 读取当前日志文件失败: " + e.getMessage());
                lastException = e;
            }
        }
        
        // 如果所有字符集都失败，尝试按字节读取文件
        if (!readSuccess) {
            try {
                debug("尝试按字节读取当前日志文件");
                allLines = readFileByBytes(logFile);
                readSuccess = true;
                debug("按字节读取当前日志文件成功");
            } catch (Exception e) {
                debug("按字节读取当前日志文件失败: " + e.getMessage());
                lastException = e;
            }
        }
        
        // 如果仍然失败，返回错误信息
        if (!readSuccess) {
            error("无法以任何编码方式读取当前日志文件", lastException);
            return new String[]{"无法读取日志文件: " + (lastException != null ? lastException.getMessage() : "未知错误")};
        }
        
        // 如果文件为空
        if (allLines.isEmpty()) {
            return new String[]{"日志文件为空"};
        }
        
        // 如果请求的行数大于实际行数，返回所有行
        if (lines >= allLines.size()) {
            return allLines.toArray(new String[0]);
        }
        
        // 否则返回最后的n行
        return allLines.subList(allLines.size() - lines, allLines.size())
                     .toArray(new String[0]);
    }
    
    /**
     * 使用指定字符集读取文件的所有行
     * @param file 要读取的文件
     * @param charset 字符集
     * @return 文件内容的行列表
     * @throws IOException 如果读取失败
     */
    private static java.util.List<String> readLinesWithCharset(File file, java.nio.charset.Charset charset) throws IOException {
        return java.nio.file.Files.readAllLines(file.toPath(), charset);
    }
    
    /**
     * 按字节读取文件，跳过无法解析的字符
     * @param file 要读取的文件
     * @return 文件内容的行列表
     * @throws IOException 如果读取失败
     */
    private static java.util.List<String> readFileByBytes(File file) throws IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            int b;
            while ((b = fis.read()) != -1) {
                // 如果是换行符，添加当前行并重置
                if (b == '\n') {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                } 
                // 如果是可打印字符，添加到当前行
                else if (b >= 32 && b < 127) {
                    currentLine.append((char) b);
                }
                // 跳过其他无法打印的字符
            }
            
            // 添加最后一行（如果有）
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        return lines;
    }
    
    /**
     * 获取可用的日志文件列表
     * @return 日志文件列表
     */
    public static File[] getLogFiles() {
        if (logFile == null) {
            File pluginDataFolder = plugin.getDataFolder();
            if (!pluginDataFolder.exists()) {
                return new File[0];
            }
            
            File logsDir = new File(pluginDataFolder, "logs");
            if (!logsDir.exists() || !logsDir.isDirectory()) {
                return new File[0];
            }
            
            return logsDir.listFiles((dir, name) -> name.endsWith(".log"));
        }
        
        File logsDir = logFile.getParentFile();
        if (logsDir != null && logsDir.exists() && logsDir.isDirectory()) {
            File[] files = logsDir.listFiles((dir, name) -> name.endsWith(".log"));
            if (files == null) {
                return new File[0];
            }
            return files;
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
        if (date == null || date.isEmpty()) {
            return new String[]{"无效的日期格式"};
        }
        
        // 验证日期格式
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return new String[]{"无效的日期格式，请使用 yyyy-MM-dd 格式"};
        }
        
        // 获取日志目录
        File logsDir;
        if (logFile != null) {
            logsDir = logFile.getParentFile();
        } else {
            logsDir = new File(plugin.getDataFolder(), "logs");
        }
        
        if (!logsDir.exists() || !logsDir.isDirectory()) {
            return new String[]{"日志目录不存在或不可访问"};
        }
        
        debug("正在查找日期为 " + date + " 的日志文件");
        
        // 创建目标日志文件对象
        File targetLog = new File(logsDir, date + ".log");
        debug("目标日志文件路径: " + targetLog.getAbsolutePath());
        
        if (!targetLog.exists()) {
            debug("目标日志文件不存在: " + targetLog.getAbsolutePath());
            return new String[]{"找不到指定日期的日志文件: " + date};
        }
        
        if (!targetLog.isFile()) {
            debug("目标路径不是文件: " + targetLog.getAbsolutePath());
            return new String[]{"指定路径不是有效的文件"};
        }
        
        // 确保日志文件可读
        if (!targetLog.canRead()) {
            debug("目标日志文件不可读: " + targetLog.getAbsolutePath());
            return new String[]{"无法读取日志文件：权限不足"};
        }
        
        debug("文件存在且可读，正在读取内容");
        
        // 限制最大行数
        lines = Math.min(lines, MAX_LOG_LINES);
        
        // 尝试不同的字符集读取文件
        java.util.List<String> allLines = new java.util.ArrayList<>();
        java.nio.charset.Charset[] charsets = {
            StandardCharsets.UTF_8,
            StandardCharsets.ISO_8859_1,
            java.nio.charset.Charset.forName("GBK"),
            java.nio.charset.Charset.forName("GB2312"),
            java.nio.charset.Charset.defaultCharset()
        };
        
        Exception lastException = null;
        boolean readSuccess = false;
        
        for (java.nio.charset.Charset charset : charsets) {
            try {
                debug("尝试使用字符集 " + charset.name() + " 读取文件");
                allLines = readLinesWithCharset(targetLog, charset);
                readSuccess = true;
                debug("使用 " + charset.name() + " 成功读取文件");
                break;
            } catch (Exception e) {
                debug("使用 " + charset.name() + " 读取文件失败: " + e.getMessage());
                lastException = e;
            }
        }
        
        // 如果所有字符集都失败，尝试按字节读取文件
        if (!readSuccess) {
            try {
                debug("尝试按字节读取文件");
                allLines = readFileByBytes(targetLog);
                readSuccess = true;
                debug("按字节读取文件成功");
            } catch (Exception e) {
                debug("按字节读取文件失败: " + e.getMessage());
                lastException = e;
            }
        }
        
        // 如果仍然失败，返回错误信息
        if (!readSuccess || allLines.isEmpty()) {
            error("无法以任何编码方式读取日志文件", lastException);
            return new String[]{"无法读取日志文件: " + (lastException != null ? lastException.getMessage() : "未知错误")};
        }
        
        debug("成功读取文件，共 " + allLines.size() + " 行");
        
        // 如果文件为空
        if (allLines.isEmpty()) {
            return new String[]{"日志文件为空"};
        }
        
        if (lines >= allLines.size()) {
            debug("返回所有行: " + allLines.size() + " 行");
            return allLines.toArray(new String[0]);
        }
        
        debug("返回最后 " + lines + " 行");
        return allLines.subList(allLines.size() - lines, allLines.size())
                     .toArray(new String[0]);
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
                String fileName = file.getName();
                // 确保文件名长度足够再进行截取
                if (fileName.length() < 10) {
                    warn("无效的日志文件名格式: " + fileName);
                    continue;
                }
                
                String dateStr = fileName.substring(0, 10);
                Date fileDate = fileNameFormat.parse(dateStr);
                
                if (fileDate.getTime() < cutoffTime && !file.equals(logFile)) {
                    if (file.delete()) {
                        deletedCount++;
                    }
                }
            } catch (ParseException e) {
                warn("无法解析日志文件日期: " + file.getName());
            } catch (Exception e) {
                warn("处理日志文件时出错: " + file.getName() + ", 错误: " + e.getMessage());
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
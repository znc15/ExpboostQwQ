package org.littlesheep.expboostQwQ.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.littlesheep.expboostQwQ.ExpboostQwQ;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 语言管理器类
 * 用于加载和管理多语言支持
 */
public class LanguageManager {
    private final ExpboostQwQ plugin;
    private final Map<String, FileConfiguration> languages = new HashMap<>();
    private String defaultLanguage = "zh_CN";
    private File langFolder;
    
    /**
     * 构造函数
     * @param plugin 插件主类实例
     */
    public LanguageManager(ExpboostQwQ plugin) {
        this.plugin = plugin;
        this.langFolder = new File(plugin.getDataFolder(), "lang");
        this.defaultLanguage = plugin.getConfig().getString("settings.default_language", "zh_CN");
        
        // 初始化语言文件夹
        initLangFolder();
        
        // 加载所有语言文件
        loadLanguages();
        
        LogUtil.info("已加载 " + languages.size() + " 个语言文件，默认语言: " + defaultLanguage);
    }
    
    /**
     * 初始化语言文件夹，保存默认语言文件
     */
    private void initLangFolder() {
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            LogUtil.error("无法创建语言文件夹: " + langFolder.getPath());
            return;
        }
        
        // 保存默认语言文件
        saveDefaultLanguageFile("zh_CN.yml");
        saveDefaultLanguageFile("en_US.yml");
    }
    
    /**
     * 保存默认语言文件
     * @param fileName 文件名
     */
    private void saveDefaultLanguageFile(String fileName) {
        File file = new File(langFolder, fileName);
        if (!file.exists()) {
            try (InputStream is = plugin.getResource("lang/" + fileName)) {
                if (is != null) {
                    YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8))
                            .save(file);
                    LogUtil.info("保存默认语言文件: " + fileName);
                } else {
                    LogUtil.warn("默认语言文件不存在: " + fileName);
                }
            } catch (IOException e) {
                LogUtil.error("保存默认语言文件失败: " + fileName, e);
            }
        }
    }
    
    /**
     * 加载所有语言文件
     */
    private void loadLanguages() {
        languages.clear();
        
        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            LogUtil.error("无法读取语言文件夹: " + langFolder.getPath());
            return;
        }
        
        for (File file : files) {
            String langCode = file.getName().replace(".yml", "");
            FileConfiguration langConfig = YamlConfiguration.loadConfiguration(file);
            languages.put(langCode, langConfig);
            LogUtil.debug("加载语言文件: " + langCode);
        }
        
        // 如果默认语言不存在，尝试使用第一个可用的语言
        if (!languages.containsKey(defaultLanguage) && !languages.isEmpty()) {
            defaultLanguage = languages.keySet().iterator().next();
            LogUtil.warn("默认语言 '" + plugin.getConfig().getString("settings.default_language") + 
                    "' 不可用，使用 '" + defaultLanguage + "' 作为默认语言");
        }
    }
    
    /**
     * 获取指定语言代码的语言配置
     * @param langCode 语言代码
     * @return 语言配置
     */
    public FileConfiguration getLanguage(String langCode) {
        return languages.getOrDefault(langCode, languages.get(defaultLanguage));
    }
    
    /**
     * 获取消息文本
     * @param langCode 语言代码
     * @param path 消息路径
     * @param defaultMessage 默认消息（如果未找到）
     * @return 消息文本
     */
    public String getMessage(String langCode, String path, String defaultMessage) {
        FileConfiguration langConfig = getLanguage(langCode);
        return langConfig != null ? langConfig.getString(path, defaultMessage) : defaultMessage;
    }
    
    /**
     * 获取默认语言的消息文本
     * @param path 消息路径
     * @param defaultMessage 默认消息（如果未找到）
     * @return 消息文本
     */
    public String getMessage(String path, String defaultMessage) {
        return getMessage(defaultLanguage, path, defaultMessage);
    }
    
    /**
     * 获取默认语言的消息文本，替换占位符
     * @param path 消息路径
     * @param defaultMessage 默认消息（如果未找到）
     * @param replacements 替换参数，格式为 {key1, value1, key2, value2, ...}
     * @return 替换后的消息文本
     */
    public String getMessage(String path, String defaultMessage, String... replacements) {
        String message = getMessage(path, defaultMessage);
        
        if (replacements != null && replacements.length >= 2) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        return message;
    }
    
    /**
     * 获取默认语言代码
     * @return 默认语言代码
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    /**
     * 设置服务器默认语言
     * 
     * @param langCode 语言代码
     */
    public void setDefaultLanguage(String langCode) {
        if (!languages.containsKey(langCode)) {
            plugin.getLogger().warning("尝试设置未知语言代码作为默认语言: " + langCode);
            return;
        }
        
        // 更新配置中的默认语言
        plugin.getConfig().set("language.default", langCode);
        plugin.saveConfigWithComments();
        
        // 更新当前默认语言
        defaultLanguage = langCode;
        
        plugin.getLogger().info("服务器默认语言已设置为: " + langCode);
    }
    
    /**
     * 获取所有可用的语言代码
     * @return 语言代码集合
     */
    public Map<String, String> getAvailableLanguages() {
        Map<String, String> result = new HashMap<>();
        
        for (String langCode : languages.keySet()) {
            FileConfiguration langConfig = languages.get(langCode);
            String langName = langConfig.getString("language.name", langCode);
            result.put(langCode, langName);
        }
        
        return result;
    }
    
    /**
     * 重新加载所有语言文件
     */
    public void reload() {
        // 重新从配置文件获取默认语言
        this.defaultLanguage = plugin.getConfig().getString("settings.default_language", "zh_CN");
        
        // 重新加载语言文件
        loadLanguages();
        
        LogUtil.info("已重新加载 " + languages.size() + " 个语言文件，默认语言: " + defaultLanguage);
    }
    
    /**
     * 获取用户语言，如果用户没有设置则返回默认语言
     * @param uuid 用户UUID
     * @return 语言代码
     */
    public String getPlayerLanguage(String uuid) {
        FileConfiguration playerData = plugin.getConfig();
        return playerData.getString("player_languages." + uuid, defaultLanguage);
    }
    
    /**
     * 设置用户语言
     * @param uuid 用户UUID
     * @param langCode 语言代码
     */
    public void setPlayerLanguage(String uuid, String langCode) {
        if (languages.containsKey(langCode)) {
            plugin.getConfig().set("player_languages." + uuid, langCode);
            plugin.saveConfigWithComments();
            LogUtil.debug("已为用户 " + uuid + " 设置语言: " + langCode);
        } else {
            LogUtil.warn("无法为用户 " + uuid + " 设置语言 '" + langCode + "'，该语言不存在");
        }
    }
} 
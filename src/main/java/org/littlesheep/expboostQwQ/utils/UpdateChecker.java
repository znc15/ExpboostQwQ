package org.littlesheep.expboostQwQ.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.littlesheep.expboostQwQ.ExpboostQwQ;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 更新检查器工具类，用于检查插件更新
 */
public class UpdateChecker {

    private final ExpboostQwQ plugin;
    private final String currentVersion;
    private final String updateUrl;
    private String latestVersion;
    private boolean updateAvailable = false;

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public UpdateChecker(ExpboostQwQ plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.updateUrl = plugin.getConfig().getString("settings.update_url", "https://api.tcbmc.cc/update/ExpBooster/update.json");
    }

    /**
     * 异步检查更新
     */
    public void checkForUpdates() {
        if (!plugin.getConfig().getBoolean("settings.check_update", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(updateUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "ExpboostQwQ/" + currentVersion);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int statusCode = connection.getResponseCode();
                if (statusCode != 200) {
                    String errorMsg = plugin.getLanguageManager().getMessage(
                            plugin.getLanguageManager().getDefaultLanguage(),
                            "messages.command.update_check_error",
                            "§c[ExpboostQwQ] 检查更新时发生错误: %error%")
                            .replace("%error%", "状态码: " + statusCode);
                    LogUtil.error(errorMsg);
                    return;
                }
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    // 使用Gson直接解析JSON
                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
                    latestVersion = jsonObject.get("version").getAsString();
                    
                    if (!currentVersion.equals(latestVersion)) {
                        updateAvailable = true;
                        
                        String updateAvailableMsg = plugin.getLanguageManager().getMessage(
                                plugin.getLanguageManager().getDefaultLanguage(),
                                "messages.command.update_available",
                                "§b[ExpboostQwQ] §f检测到新版本: §a%latest_version% §f(当前版本: §c%current_version%§f)")
                                .replace("%latest_version%", latestVersion)
                                .replace("%current_version%", currentVersion);
                        
                        LogUtil.info(updateAvailableMsg);
                        
                        String downloadLinkMsg = plugin.getLanguageManager().getMessage(
                                plugin.getLanguageManager().getDefaultLanguage(),
                                "messages.command.update_download_link",
                                "§b[ExpboostQwQ] §f请前往 §ehttps://github.com/znc15/ExpboostQwQ/releases §f下载最新版本");
                        
                        LogUtil.info(downloadLinkMsg);
                        
                        // 通知在线管理员
                        Bukkit.getOnlinePlayers().stream()
                                .filter(player -> player.hasPermission("expboostqwq.admin"))
                                .forEach(player -> {
                                    String langCode = plugin.getLanguageManager().getPlayerLanguage(player.getUniqueId().toString());
                                    
                                    String playerUpdateMsg = plugin.getLanguageManager().getMessage(
                                            langCode,
                                            "messages.command.update_available",
                                            "§b[ExpboostQwQ] §f检测到新版本: §a%latest_version% §f(当前版本: §c%current_version%§f)")
                                            .replace("%latest_version%", latestVersion)
                                            .replace("%current_version%", currentVersion);
                                    
                                    player.sendMessage(playerUpdateMsg);
                                    
                                    String playerDownloadMsg = plugin.getLanguageManager().getMessage(
                                            langCode,
                                            "messages.command.update_download_link",
                                            "§b[ExpboostQwQ] §f请前往 §ehttps://github.com/znc15/ExpboostQwQ/releases §f下载最新版本");
                                    
                                    player.sendMessage(playerDownloadMsg);
                                });
                    } else {
                        String noUpdateMsg = plugin.getLanguageManager().getMessage(
                                plugin.getLanguageManager().getDefaultLanguage(),
                                "messages.command.no_update_available",
                                "§b[ExpboostQwQ] §f你正在使用最新版本 (v%current_version%)")
                                .replace("%current_version%", currentVersion);
                        
                        LogUtil.info(noUpdateMsg);
                    }
                }
            } catch (Exception e) {
                String errorMsg = plugin.getLanguageManager().getMessage(
                        plugin.getLanguageManager().getDefaultLanguage(),
                        "messages.command.update_check_error",
                        "§c[ExpboostQwQ] 检查更新时发生错误: %error%")
                        .replace("%error%", e.getMessage());
                
                LogUtil.error(errorMsg, e);
            }
        });
    }

    /**
     * 获取最新版本号
     * @return 最新版本号
     */
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
     * 检查是否有可用更新
     * @return 是否有可用更新
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
} 
package org.littlesheep.expboostQwQ.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.littlesheep.expboostQwQ.ExpboostQwQ;
import org.littlesheep.expboostQwQ.utils.LogUtil;
import org.littlesheep.expboostQwQ.utils.LevelApiUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 经验加成管理器
 * 负责管理全服和玩家的经验加成数据，包括存储、加载和计算最终加成效果
 */
public class BoosterManager {
    
    private final ExpboostQwQ plugin;
    // 使用线程安全的ConcurrentHashMap存储玩家加成数据
    private final Map<UUID, PlayerBooster> playerBoosters = new ConcurrentHashMap<>();
    // 全服加成数据
    private ServerBooster serverBooster = null;
    // 全局默认倍率
    private PlayerBooster globalBooster = new PlayerBooster(1.0, -1, "", "");
    // 等级组特定倍率
    private final Map<String, PlayerBooster> levelGroupBoosters = new ConcurrentHashMap<>();
    // 数据文件引用
    private final File dataFile;
    // 数据文件配置引用
    private final FileConfiguration dataConfig;
    
    /**
     * 构造函数，初始化加成管理器
     * @param plugin 插件主类实例
     */
    public BoosterManager(ExpboostQwQ plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "boosters.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // 加载数据
        loadData();
        
        // 启动定时任务，用于检查过期的加成
        startExpirationTask();
    }
    
    /**
     * 为指定玩家添加经验加成
     * @param uuid 玩家UUID
     * @param booster 经验加成数据
     */
    public void addPlayerBooster(UUID uuid, PlayerBooster booster) {
        playerBoosters.put(uuid, booster);
        saveAll();
        
        // 记录日志
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
            LogUtil.playerBooster(player, "设置倍率 " + booster.getMultiplier() + "x，持续时间 " + 
                    (booster.getEndTime() == -1 ? "永久" : booster.getFormattedTimeLeft()));
        }
    }
    
    /**
     * 移除指定玩家的经验加成
     * @param uuid 玩家UUID
     */
    public void removePlayerBooster(UUID uuid) {
        // 记录日志
        if (playerBoosters.containsKey(uuid) && plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                LogUtil.playerBooster(player, "移除经验加成");
            }
        }
        
        playerBoosters.remove(uuid);
        saveAll();
    }
    
    /**
     * 获取指定玩家的经验加成数据
     * @param uuid 玩家UUID
     * @return 玩家的经验加成数据，如果不存在则返回null
     */
    public PlayerBooster getPlayerBooster(UUID uuid) {
        return playerBoosters.get(uuid);
    }
    
    /**
     * 检查玩家是否有活跃的经验加成
     * @param uuid 玩家UUID
     * @return 如果玩家有未过期的加成则返回true，否则返回false
     */
    public boolean hasPlayerBooster(UUID uuid) {
        return playerBoosters.containsKey(uuid) && playerBoosters.get(uuid).isActive();
    }
    
    /**
     * 设置全服经验加成
     * @param booster 全服经验加成数据
     */
    public void setServerBooster(ServerBooster booster) {
        this.serverBooster = booster;
        saveAll();
        
        // 记录日志
        if (plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
            LogUtil.serverBooster("设置倍率 " + booster.getMultiplier() + "x，持续时间 " + 
                    (booster.getEndTime() == -1 ? "永久" : booster.getFormattedTimeLeft()));
        }
    }
    
    /**
     * 获取全服经验加成数据
     * @return 全服经验加成数据，如果不存在则返回null
     */
    public ServerBooster getServerBooster() {
        return serverBooster;
    }
    
    /**
     * 检查是否有活跃的全服经验加成
     * @return 如果有未过期的全服加成则返回true，否则返回false
     */
    public boolean hasActiveServerBooster() {
        return serverBooster != null && serverBooster.isActive();
    }
    
    /**
     * 设置全局默认倍率
     * @param multiplier 默认倍率
     * @param duration 持续时间（秒），-1表示永久
     */
    public void setGlobalDefaultMultiplier(double multiplier, long duration) {
        long endTime = (duration == -1) ? -1 : System.currentTimeMillis() + (duration * 1000);
        this.globalBooster = new PlayerBooster(multiplier, endTime, "", "");
        plugin.getConfig().set("multipliers.global_default.multiplier", multiplier);
        plugin.getConfig().set("multipliers.global_default.end_time", endTime);
        plugin.saveConfig();
        
        // 记录日志
        if (plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
            String durationStr = duration == -1 ? "永久" : duration + "秒";
            LogUtil.info("全局默认倍率已设置为 " + multiplier + "x，持续时间: " + durationStr);
        }
    }
    
    /**
     * 获取全局默认倍率
     * @return 全局默认倍率，如果未设置或已过期则返回1.0
     */
    public double getGlobalDefaultMultiplier() {
        return globalBooster.isActive() ? globalBooster.getMultiplier() : 1.0;
    }
    
    /**
     * 获取全局默认倍率加成对象
     * @return 全局默认倍率加成对象
     */
    public PlayerBooster getGlobalBooster() {
        return globalBooster;
    }
    
    /**
     * 设置特定等级组的倍率
     * @param levelGroup 等级组名称
     * @param multiplier 倍率
     * @param duration 持续时间（秒），-1表示永久
     * @return 是否设置成功
     */
    public boolean setLevelGroupMultiplier(String levelGroup, double multiplier, long duration) {
        // 验证等级组是否存在
        if (!LevelApiUtil.isLevelGroupExists(levelGroup)) {
            return false;
        }
        
        // 计算结束时间
        long endTime = (duration == -1) ? -1 : System.currentTimeMillis() + (duration * 1000);
        
        // 创建加成对象
        PlayerBooster booster = new PlayerBooster(multiplier, endTime, levelGroup, "");
        levelGroupBoosters.put(levelGroup, booster);
        
        // 保存到配置文件
        plugin.getConfig().set("multipliers.level_groups." + levelGroup + ".multiplier", multiplier);
        plugin.getConfig().set("multipliers.level_groups." + levelGroup + ".end_time", endTime);
        plugin.saveConfig();
        
        // 记录日志
        if (plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
            String durationStr = duration == -1 ? "永久" : duration + "秒";
            LogUtil.info("等级组 " + levelGroup + " 的倍率已设置为 " + multiplier + "x，持续时间: " + durationStr);
        }
        
        return true;
    }
    
    /**
     * 获取特定等级组的倍率
     * @param levelGroup 等级组名称
     * @return 等级组倍率，如果未设置或已过期则返回全局默认倍率
     */
    public double getLevelGroupMultiplier(String levelGroup) {
        PlayerBooster booster = levelGroupBoosters.get(levelGroup);
        if (booster != null && booster.isActive()) {
            return booster.getMultiplier();
        }
        return globalBooster.isActive() ? globalBooster.getMultiplier() : 1.0;
    }
    
    /**
     * 获取所有等级组倍率
     * @return 等级组倍率映射
     */
    public Map<String, PlayerBooster> getAllLevelGroupBoosters() {
        return new HashMap<>(levelGroupBoosters);
    }
    
    /**
     * 获取玩家在特定等级组和经验来源下的有效经验加成倍率
     * 会同时考虑玩家个人加成、全服加成和默认倍率，并将它们相乘得到最终倍率
     * 
     * @param player 玩家对象
     * @param levelGroup 等级组名称
     * @param source 经验来源
     * @return 最终的经验加成倍率
     */
    public double getEffectiveMultiplier(Player player, String levelGroup, String source) {
        // 初始倍率为1.0
        double multiplier = 1.0;
        
        // 应用等级组特定倍率，优先级高于全局默认倍率
        if (levelGroupBoosters.containsKey(levelGroup)) {
            PlayerBooster booster = levelGroupBoosters.get(levelGroup);
            if (booster.isActive()) {
                multiplier *= booster.getMultiplier();
                LogUtil.debug("应用等级组 " + levelGroup + " 倍率: " + booster.getMultiplier());
            }
        } else {
            // 应用全局默认倍率
            multiplier *= globalBooster.isActive() ? globalBooster.getMultiplier() : 1.0;
            LogUtil.debug("应用全局默认倍率: " + multiplier);
        }
        
        // 检查服务器全局加成
        if (hasActiveServerBooster()) {
            ServerBooster sb = getServerBooster();
            if (sb.matchesConditions(levelGroup, source)) {
                multiplier *= sb.getMultiplier();
                LogUtil.debug("应用服务器加成倍率: " + sb.getMultiplier());
            }
        }
        
        // 检查玩家个人加成
        if (hasPlayerBooster(player.getUniqueId())) {
            PlayerBooster pb = getPlayerBooster(player.getUniqueId());
            if (pb.matchesConditions(levelGroup, source)) {
                multiplier *= pb.getMultiplier();
                LogUtil.debug("应用玩家个人加成倍率: " + pb.getMultiplier());
            }
        }
        
        LogUtil.debug("玩家 " + player.getName() + " 在等级组 " + levelGroup + 
                " 来源 " + source + " 的最终倍率: " + multiplier);
        
        return multiplier;
    }
    
    /**
     * 从配置文件加载经验加成数据
     */
    public void loadData() {
        // 清除现有数据
        playerBoosters.clear();
        serverBooster = null;
        
        // 加载全局默认倍率
        double globalMultiplier = plugin.getConfig().getDouble("multipliers.global_default.multiplier", 1.0);
        long globalEndTime = plugin.getConfig().getLong("multipliers.global_default.end_time", -1);
        globalBooster = new PlayerBooster(globalMultiplier, globalEndTime, "", "");
        if (!globalBooster.isActive()) {
            globalBooster = new PlayerBooster(1.0, -1, "", "");
        }
        
        // 加载玩家加成数据
        if (dataConfig.contains("player_boosters")) {
            for (String uuidString : dataConfig.getConfigurationSection("player_boosters").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                double playerMultiplier = dataConfig.getDouble("player_boosters." + uuidString + ".multiplier");
                long playerEndTime = dataConfig.getLong("player_boosters." + uuidString + ".end_time");
                String levelGroup = dataConfig.getString("player_boosters." + uuidString + ".level_group", "");
                String source = dataConfig.getString("player_boosters." + uuidString + ".source", "");
                
                PlayerBooster booster = new PlayerBooster(playerMultiplier, playerEndTime, levelGroup, source);
                if (booster.isActive()) {
                    playerBoosters.put(uuid, booster);
                }
            }
        }
        
        // 加载服务器加成数据
        if (dataConfig.contains("server_booster")) {
            double serverMultiplier = dataConfig.getDouble("server_booster.multiplier");
            long serverEndTime = dataConfig.getDouble("server_booster.end_time") > 0 ? 
                    dataConfig.getLong("server_booster.end_time") : -1;
            String levelGroup = dataConfig.getString("server_booster.level_group", "");
            String source = dataConfig.getString("server_booster.source", "");
            
            serverBooster = new ServerBooster(serverMultiplier, serverEndTime, levelGroup, source);
            if (!serverBooster.isActive()) {
                serverBooster = null;
            }
        }
        
        // 加载等级组倍率数据
        if (plugin.getConfig().contains("multipliers.level_groups")) {
            levelGroupBoosters.clear();
            for (String group : plugin.getConfig().getConfigurationSection("multipliers.level_groups").getKeys(false)) {
                double groupMultiplier = plugin.getConfig().getDouble("multipliers.level_groups." + group + ".multiplier", 1.0);
                long groupEndTime = plugin.getConfig().getLong("multipliers.level_groups." + group + ".end_time", -1);
                
                PlayerBooster booster = new PlayerBooster(groupMultiplier, groupEndTime, group, "");
                if (booster.isActive()) {
                    levelGroupBoosters.put(group, booster);
                }
            }
        }
        
        LogUtil.info("已加载 " + playerBoosters.size() + " 个玩家加成数据和 " + 
                (serverBooster != null ? "1" : "0") + " 个全服加成数据");
    }
    
    /**
     * 保存所有经验加成数据到配置文件
     */
    public void saveAll() {
        // 保存玩家加成数据
        dataConfig.set("player_boosters", null);  // 清除旧数据
        
        for (Map.Entry<UUID, PlayerBooster> entry : playerBoosters.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerBooster booster = entry.getValue();
            
            if (booster.isActive()) {  // 只保存活跃的加成
                String path = "player_boosters." + uuid;
                dataConfig.set(path + ".multiplier", booster.getMultiplier());
                dataConfig.set(path + ".end_time", booster.getEndTime());
                dataConfig.set(path + ".level_group", booster.getLevelGroup());
                dataConfig.set(path + ".source", booster.getSource());
            }
        }
        
        // 保存服务器加成数据
        if (serverBooster != null && serverBooster.isActive()) {
            dataConfig.set("server_booster.multiplier", serverBooster.getMultiplier());
            dataConfig.set("server_booster.end_time", serverBooster.getEndTime());
            dataConfig.set("server_booster.level_group", serverBooster.getLevelGroup());
            dataConfig.set("server_booster.source", serverBooster.getSource());
        } else {
            dataConfig.set("server_booster", null);
        }
        
        // 保存等级组倍率数据
        for (Map.Entry<String, PlayerBooster> entry : levelGroupBoosters.entrySet()) {
            String group = entry.getKey();
            PlayerBooster booster = entry.getValue();
            
            if (booster.isActive()) {
                plugin.getConfig().set("multipliers.level_groups." + group + ".multiplier", booster.getMultiplier());
                plugin.getConfig().set("multipliers.level_groups." + group + ".end_time", booster.getEndTime());
            } else {
                plugin.getConfig().set("multipliers.level_groups." + group, null);
            }
        }
        
        // 保存全局默认倍率
        if (globalBooster.isActive() && globalBooster.getMultiplier() != 1.0) {
            plugin.getConfig().set("multipliers.global_default.multiplier", globalBooster.getMultiplier());
            plugin.getConfig().set("multipliers.global_default.end_time", globalBooster.getEndTime());
        } else {
            plugin.getConfig().set("multipliers.global_default", null);
        }
        
        plugin.saveConfig();
        
        // 保存到文件
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            LogUtil.error("无法保存加成数据: " + e.getMessage(), e);
        }
    }
    
    /**
     * 启动定期检查过期加成的任务
     * 每分钟运行一次，移除过期的加成并保存数据
     */
    private void startExpirationTask() {
        // 每分钟检查一次过期的加成
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            boolean changed = false;
            
            // 检查玩家加成
            for (UUID uuid : new HashMap<>(playerBoosters).keySet()) {
                PlayerBooster booster = playerBoosters.get(uuid);
                if (!booster.isActive()) {
                    playerBoosters.remove(uuid);
                    changed = true;
                    
                    // 记录日志
                    if (plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            LogUtil.playerBooster(player, "加成已过期并自动移除");
                        }
                    }
                }
            }
            
            // 检查服务器加成
            if (serverBooster != null && !serverBooster.isActive()) {
                // 记录日志
                if (plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
                    LogUtil.serverBooster("全服加成已过期并自动移除");
                }
                
                serverBooster = null;
                changed = true;
            }
            
            // 检查等级组加成
            for (String group : new HashMap<>(levelGroupBoosters).keySet()) {
                PlayerBooster booster = levelGroupBoosters.get(group);
                if (!booster.isActive()) {
                    levelGroupBoosters.remove(group);
                    changed = true;
                    
                    // 记录日志
                    if (plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
                        LogUtil.info("等级组 " + group + " 的倍率加成已过期并自动移除");
                    }
                }
            }
            
            // 检查全局默认倍率
            if (!globalBooster.isActive() && globalBooster.getMultiplier() != 1.0) {
                globalBooster = new PlayerBooster(1.0, -1, "", "");
                changed = true;
                
                // 记录日志
                if (plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
                    LogUtil.info("全局默认倍率加成已过期并重置为1.0x");
                }
            }
            
            // 如果有变更，保存数据
            if (changed) {
                saveAll();
            }
        }, 1200L, 1200L);  // 每分钟运行一次
    }
} 
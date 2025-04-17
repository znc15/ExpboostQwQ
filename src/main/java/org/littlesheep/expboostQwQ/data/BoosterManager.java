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
import java.util.List;
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
    private final Map<UUID, MultiplePlayerBoosters> playerBoosters = new ConcurrentHashMap<>();
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
        // 获取或创建玩家的多重加成容器
        MultiplePlayerBoosters multiplePlayerBoosters = playerBoosters.computeIfAbsent(
                uuid, id -> new MultiplePlayerBoosters(id));
        
        // 添加新的加成
        multiplePlayerBoosters.addBooster(booster);
        saveAll();
        
        // 记录日志
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
            LogUtil.playerBooster(player, "添加倍率 " + booster.getMultiplier() + "x，持续时间 " + 
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
                LogUtil.playerBooster(player, "移除所有经验加成");
            }
        }
        
        playerBoosters.remove(uuid);
        saveAll();
    }
    
    /**
     * 移除玩家的特定加成
     * @param uuid 玩家UUID
     * @param index 加成索引
     * @return 是否成功移除
     */
    public boolean removePlayerBoosterByIndex(UUID uuid, int index) {
        if (!playerBoosters.containsKey(uuid)) {
            return false;
        }
        
        MultiplePlayerBoosters boosters = playerBoosters.get(uuid);
        boolean removed = boosters.removeBooster(index);
        
        if (removed) {
            // 如果没有剩余加成，完全移除
            if (boosters.getBoosterCount() == 0) {
                playerBoosters.remove(uuid);
            }
            saveAll();
            
            // 记录日志
            if (plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    LogUtil.playerBooster(player, "移除第 " + (index + 1) + " 个经验加成");
                }
            }
        }
        
        return removed;
    }
    
    /**
     * 获取指定玩家的经验加成管理器
     * @param uuid 玩家UUID
     * @return 玩家的经验加成管理器，如果不存在则返回null
     */
    public MultiplePlayerBoosters getPlayerBoosters(UUID uuid) {
        return playerBoosters.get(uuid);
    }
    
    /**
     * 获取指定玩家的经验加成数据（兼容旧API，返回第一个活跃加成）
     * @param uuid 玩家UUID
     * @return 玩家的第一个经验加成数据，如果不存在则返回null
     */
    public PlayerBooster getPlayerBooster(UUID uuid) {
        MultiplePlayerBoosters boosters = playerBoosters.get(uuid);
        if (boosters != null && boosters.hasActiveBoosters()) {
            List<PlayerBooster> active = boosters.getActiveBoosters();
            return active.isEmpty() ? null : active.get(0);
        }
        return null;
    }
    
    /**
     * 检查玩家是否有活跃的经验加成
     * @param uuid 玩家UUID
     * @return 如果玩家有未过期的加成则返回true，否则返回false
     */
    public boolean hasPlayerBooster(UUID uuid) {
        MultiplePlayerBoosters boosters = playerBoosters.get(uuid);
        return boosters != null && boosters.hasActiveBoosters();
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
        
        // 保存到数据文件而不是配置文件
        dataConfig.set("global_default.multiplier", multiplier);
        dataConfig.set("global_default.end_time", endTime);
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            LogUtil.error("无法保存全局默认倍率数据: " + e.getMessage(), e);
        }
        
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
        
        // 保存到数据文件而不是配置文件
        dataConfig.set("level_groups." + levelGroup + ".multiplier", multiplier);
        dataConfig.set("level_groups." + levelGroup + ".end_time", endTime);
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            LogUtil.error("无法保存等级组倍率数据: " + e.getMessage(), e);
            return false;
        }
        
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
     * 会根据配置文件设置的计算方式来计算最终倍率
     * 
     * @param player 玩家对象
     * @param levelGroup 等级组名称
     * @param source 经验来源
     * @return 最终的经验加成倍率
     */
    public double getEffectiveMultiplier(Player player, String levelGroup, String source) {
        // 获取加成计算方式：highest(取最高) 或 multiply(相乘) 或 add(相加)
        String calculationType = plugin.getConfig().getString("settings.boost_calculation", "multiply");
        
        // 初始倍率为1.0
        double multiplier = 1.0;
        double maxMultiplier = 1.0;
        double addMultiplier = 0.0;
        
        // 从全局默认倍率、等级组倍率、全服加成和玩家加成获取所有适用的倍率值
        
        // 1. 获取等级组倍率或全局默认倍率
        double levelGroupMultiplier = 1.0;
        if (levelGroupBoosters.containsKey(levelGroup)) {
            PlayerBooster booster = levelGroupBoosters.get(levelGroup);
            if (booster.isActive()) {
                levelGroupMultiplier = booster.getMultiplier();
                LogUtil.debug("应用等级组 " + levelGroup + " 倍率: " + levelGroupMultiplier);
            }
        } else {
            // 应用全局默认倍率
            levelGroupMultiplier = globalBooster.isActive() ? globalBooster.getMultiplier() : 1.0;
            LogUtil.debug("应用全局默认倍率: " + levelGroupMultiplier);
        }
        
        // 2. 获取全服加成倍率
        double serverMultiplier = 1.0;
        if (hasActiveServerBooster()) {
            ServerBooster sb = getServerBooster();
            if (sb.matchesConditions(levelGroup, source)) {
                serverMultiplier = sb.getMultiplier();
                LogUtil.debug("应用服务器加成倍率: " + serverMultiplier);
            }
        }
        
        // 3. 获取玩家个人加成倍率（现在处理多个加成）
        double playerMultiplier = 1.0;
        double playerMaxMultiplier = 1.0;
        double playerAddMultiplier = 0.0;
        
        if (hasPlayerBooster(player.getUniqueId())) {
            MultiplePlayerBoosters boosters = getPlayerBoosters(player.getUniqueId());
            List<PlayerBooster> matchingBoosters = boosters.getMatchingBoosters(levelGroup, source);
            
            // 不同玩家加成之间也使用相同的计算规则
            if (!matchingBoosters.isEmpty()) {
                if (calculationType.equalsIgnoreCase("highest")) {
                    // 取最高倍率
                    for (PlayerBooster pb : matchingBoosters) {
                        playerMaxMultiplier = Math.max(playerMaxMultiplier, pb.getMultiplier());
                    }
                    playerMultiplier = playerMaxMultiplier;
                    LogUtil.debug("玩家加成计算方式: 取最高倍率 = " + playerMultiplier);
                } else if (calculationType.equalsIgnoreCase("add")) {
                    // 相加方式
                    for (PlayerBooster pb : matchingBoosters) {
                        playerAddMultiplier += (pb.getMultiplier() - 1.0);
                    }
                    playerMultiplier = 1.0 + playerAddMultiplier;
                    LogUtil.debug("玩家加成计算方式: 相加倍率 = " + playerMultiplier);
                } else {
                    // 默认相乘方式
                    for (PlayerBooster pb : matchingBoosters) {
                        playerMultiplier *= pb.getMultiplier();
                    }
                    LogUtil.debug("玩家加成计算方式: 相乘倍率 = " + playerMultiplier);
                }
            }
        }
        
        // 根据配置的计算方式计算最终倍率
        if (calculationType.equalsIgnoreCase("highest")) {
            // 取最高倍率
            maxMultiplier = Math.max(maxMultiplier, levelGroupMultiplier);
            maxMultiplier = Math.max(maxMultiplier, serverMultiplier);
            maxMultiplier = Math.max(maxMultiplier, playerMultiplier);
            
            multiplier = maxMultiplier;
            LogUtil.debug("计算方式: 取最高倍率 = " + multiplier);
        } else if (calculationType.equalsIgnoreCase("add")) {
            // 相加方式（减去基础的1.0后相加）
            addMultiplier = (levelGroupMultiplier - 1.0) + (serverMultiplier - 1.0) + (playerMultiplier - 1.0);
            // 最终倍率 = 基础1.0 + 所有加成的和
            multiplier = 1.0 + addMultiplier;
            LogUtil.debug("计算方式: 相加倍率 = " + multiplier);
        } else {
            // 默认相乘方式
            multiplier = levelGroupMultiplier * serverMultiplier * playerMultiplier;
            LogUtil.debug("计算方式: 相乘倍率 = " + multiplier);
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
        
        // 尝试首先从dataConfig加载全局默认倍率
        double globalMultiplier = 1.0;
        long globalEndTime = -1;
        
        if (dataConfig.contains("global_default")) {
            globalMultiplier = dataConfig.getDouble("global_default.multiplier", 1.0);
            globalEndTime = dataConfig.getLong("global_default.end_time", -1);
        } else if (plugin.getConfig().contains("multipliers.global_default")) {
            // 向后兼容：如果在数据文件中找不到，则尝试从旧的配置文件结构加载
            globalMultiplier = plugin.getConfig().getDouble("multipliers.global_default.multiplier", 1.0);
            globalEndTime = plugin.getConfig().getLong("multipliers.global_default.end_time", -1);
        }
        
        globalBooster = new PlayerBooster(globalMultiplier, globalEndTime, "", "");
        if (!globalBooster.isActive()) {
            globalBooster = new PlayerBooster(1.0, -1, "", "");
        }
        
        // 加载玩家加成数据
        if (dataConfig.contains("player_boosters")) {
            // 旧格式兼容 - 单个加成
            for (String uuidString : dataConfig.getConfigurationSection("player_boosters").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                
                // 检查是否是新格式（多个加成）
                if (dataConfig.contains("player_boosters." + uuidString + ".boosters")) {
                    // 新格式 - 多个加成
                    MultiplePlayerBoosters multipleBoosters = new MultiplePlayerBoosters(uuid);
                    
                    for (String index : dataConfig.getConfigurationSection("player_boosters." + uuidString + ".boosters").getKeys(false)) {
                        String path = "player_boosters." + uuidString + ".boosters." + index + ".";
                        double boosterMultiplier = dataConfig.getDouble(path + "multiplier");
                        long boosterEndTime = dataConfig.getLong(path + "end_time");
                        String levelGroup = dataConfig.getString(path + "level_group", "");
                        String source = dataConfig.getString(path + "source", "");
                        
                        PlayerBooster booster = new PlayerBooster(boosterMultiplier, boosterEndTime, levelGroup, source);
                        if (booster.isActive()) {
                            multipleBoosters.addBooster(booster);
                        }
                    }
                    
                    if (multipleBoosters.hasActiveBoosters()) {
                        playerBoosters.put(uuid, multipleBoosters);
                    }
                } else {
                    // 旧格式 - 单个加成
                    String path = "player_boosters." + uuidString;
                    double boosterMultiplier = dataConfig.getDouble(path + ".multiplier");
                    long boosterEndTime = dataConfig.getLong(path + ".end_time");
                    String levelGroup = dataConfig.getString(path + ".level_group", "");
                    String source = dataConfig.getString(path + ".source", "");
                    
                    PlayerBooster booster = new PlayerBooster(boosterMultiplier, boosterEndTime, levelGroup, source);
                    if (booster.isActive()) {
                        MultiplePlayerBoosters multipleBoosters = new MultiplePlayerBoosters(uuid);
                        multipleBoosters.addBooster(booster);
                        playerBoosters.put(uuid, multipleBoosters);
                    }
                }
            }
        }
        
        // 加载服务器加成数据
        if (dataConfig.contains("server_booster")) {
            double serverMultiplier = dataConfig.getDouble("server_booster.multiplier");
            long serverEndTime = dataConfig.getLong("server_booster.end_time", -1);
            String levelGroup = dataConfig.getString("server_booster.level_group", "");
            String source = dataConfig.getString("server_booster.source", "");
            
            serverBooster = new ServerBooster(serverMultiplier, serverEndTime, levelGroup, source);
            if (!serverBooster.isActive()) {
                serverBooster = null;
            }
        }
        
        // 加载等级组倍率数据
        levelGroupBoosters.clear();
        
        // 首先尝试从dataConfig加载
        if (dataConfig.contains("level_groups")) {
            for (String group : dataConfig.getConfigurationSection("level_groups").getKeys(false)) {
                double groupMultiplier = dataConfig.getDouble("level_groups." + group + ".multiplier", 1.0);
                long groupEndTime = dataConfig.getLong("level_groups." + group + ".end_time", -1);
                
                PlayerBooster booster = new PlayerBooster(groupMultiplier, groupEndTime, group, "");
                if (booster.isActive()) {
                    levelGroupBoosters.put(group, booster);
                }
            }
        } else if (plugin.getConfig().contains("multipliers.level_groups")) {
            // 向后兼容：如果在数据文件中找不到，则尝试从旧的配置文件结构加载
            for (String group : plugin.getConfig().getConfigurationSection("multipliers.level_groups").getKeys(false)) {
                double groupMultiplier = plugin.getConfig().getDouble("multipliers.level_groups." + group + ".multiplier", 1.0);
                long groupEndTime = plugin.getConfig().getLong("multipliers.level_groups." + group + ".end_time", -1);
                
                PlayerBooster booster = new PlayerBooster(groupMultiplier, groupEndTime, group, "");
                if (booster.isActive()) {
                    levelGroupBoosters.put(group, booster);
                }
            }
        }
        
        int totalBoosters = 0;
        for (MultiplePlayerBoosters boosters : playerBoosters.values()) {
            totalBoosters += boosters.getActiveBoosterCount();
        }
        
        LogUtil.info("已加载 " + totalBoosters + " 个玩家加成数据和 " + 
                (serverBooster != null ? "1" : "0") + " 个全服加成数据");
    }
    
    /**
     * 保存所有经验加成数据到配置文件
     */
    public void saveAll() {
        // 保存玩家加成数据
        dataConfig.set("player_boosters", null);  // 清除旧数据
        
        for (Map.Entry<UUID, MultiplePlayerBoosters> entry : playerBoosters.entrySet()) {
            UUID uuid = entry.getKey();
            MultiplePlayerBoosters multipleBoosters = entry.getValue();
            List<PlayerBooster> boosters = multipleBoosters.getActiveBoosters();
            
            if (!boosters.isEmpty()) {
                String basePath = "player_boosters." + uuid + ".boosters";
                
                for (int i = 0; i < boosters.size(); i++) {
                    PlayerBooster booster = boosters.get(i);
                    String path = basePath + "." + i;
                    
                    dataConfig.set(path + ".multiplier", booster.getMultiplier());
                    dataConfig.set(path + ".end_time", booster.getEndTime());
                    dataConfig.set(path + ".level_group", booster.getLevelGroup());
                    dataConfig.set(path + ".source", booster.getSource());
                }
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
                dataConfig.set("level_groups." + group + ".multiplier", booster.getMultiplier());
                dataConfig.set("level_groups." + group + ".end_time", booster.getEndTime());
            } else {
                dataConfig.set("level_groups." + group, null);
            }
        }
        
        // 保存全局默认倍率
        if (globalBooster.isActive()) {
            dataConfig.set("global_default.multiplier", globalBooster.getMultiplier());
            dataConfig.set("global_default.end_time", globalBooster.getEndTime());
        } else {
            dataConfig.set("global_default", null);
        }
        
        // 保存到文件
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            LogUtil.error("保存加成数据时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 启动定时任务，用于检查过期的加成
     */
    private void startExpirationTask() {
        // 定时检查过期的加成（每分钟）
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            boolean changed = false;
            
            // 检查玩家加成
            for (UUID uuid : new HashMap<>(playerBoosters).keySet()) {
                MultiplePlayerBoosters boosters = playerBoosters.get(uuid);
                boolean hasExpired = boosters.cleanupExpiredBoosters();
                
                if (hasExpired) {
                    changed = true;
                    
                    // 记录日志
                    if (plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            LogUtil.playerBooster(player, "部分加成已过期并自动移除");
                        }
                    }
                    
                    // 如果没有剩余加成，移除整个条目
                    if (!boosters.hasActiveBoosters()) {
                        playerBoosters.remove(uuid);
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
            if (globalBooster != null && !globalBooster.isActive()) {
                globalBooster = new PlayerBooster(1.0, -1, "", "");
                changed = true;
                
                // 记录日志
                if (plugin.getConfig().getBoolean("settings.log_exp_boost", true)) {
                    LogUtil.info("全局默认倍率加成已过期并重置为1.0x");
                }
            }
            
            // 如果有任何更改，保存数据
            if (changed) {
                saveAll();
            }
        }, 20 * 60, 20 * 60); // 每分钟检查一次（20 ticks * 60 = 1分钟）
    }
    
    /**
     * 判断特定等级组是否有加成
     * @param levelGroup 等级组名称
     * @return 是否有加成
     */
    public boolean hasLevelGroupBooster(String levelGroup) {
        PlayerBooster booster = levelGroupBoosters.get(levelGroup);
        return booster != null && booster.isActive();
    }
    
    /**
     * 移除全服加成
     * @return 是否成功移除
     */
    public boolean removeServerBooster() {
        if (serverBooster == null) {
            return false;
        }
        
        serverBooster = null;
        // 保存到数据文件
        dataConfig.set("server_booster", null);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            LogUtil.error("无法保存服务器加成数据: " + e.getMessage(), e);
            return false;
        }
        
        return true;
    }
    
    /**
     * 移除特定等级组的加成
     * @param levelGroup 等级组名称
     * @return 是否成功移除
     */
    public boolean removeLevelGroupBooster(String levelGroup) {
        if (!levelGroupBoosters.containsKey(levelGroup)) {
            return false;
        }
        
        levelGroupBoosters.remove(levelGroup);
        // 保存到数据文件而不是配置文件
        dataConfig.set("level_groups." + levelGroup, null);
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            LogUtil.error("无法保存等级组倍率数据: " + e.getMessage(), e);
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取特定等级组的加成器
     * @param levelGroup 等级组名称
     * @return 等级组加成器对象，如果不存在则返回null
     */
    public PlayerBooster getLevelGroupBooster(String levelGroup) {
        PlayerBooster booster = levelGroupBoosters.get(levelGroup);
        if (booster != null && booster.isActive()) {
            return booster;
        }
        return null;
    }
} 
package org.littlesheep.expboostQwQ.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 玩家多重经验加成管理类
 * 用于存储和管理玩家的多个经验加成
 */
public class MultiplePlayerBoosters {
    private final UUID playerUuid;
    private final List<PlayerBooster> boosters = new ArrayList<>();
    
    /**
     * 构造函数
     * 
     * @param playerUuid 玩家UUID
     */
    public MultiplePlayerBoosters(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    /**
     * 添加经验加成
     * 
     * @param booster 要添加的经验加成
     * @return 是否成功添加
     */
    public boolean addBooster(PlayerBooster booster) {
        if (booster == null || !booster.isActive()) {
            return false;
        }
        boosters.add(booster);
        return true;
    }
    
    /**
     * 获取所有活跃的经验加成
     * 
     * @return 活跃经验加成列表
     */
    public List<PlayerBooster> getActiveBoosters() {
        return boosters.stream()
                .filter(PlayerBooster::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * 清理过期的经验加成
     * 
     * @return 是否有加成被移除
     */
    public boolean cleanupExpiredBoosters() {
        int initialSize = boosters.size();
        boosters.removeIf(booster -> !booster.isActive());
        return boosters.size() < initialSize;
    }
    
    /**
     * 检查是否有活跃的经验加成
     * 
     * @return 是否有活跃加成
     */
    public boolean hasActiveBoosters() {
        return boosters.stream().anyMatch(PlayerBooster::isActive);
    }
    
    /**
     * 获取特定等级组和经验来源的有效加成
     * 
     * @param levelGroup 等级组
     * @param source 经验来源
     * @return 匹配条件的活跃加成列表
     */
    public List<PlayerBooster> getMatchingBoosters(String levelGroup, String source) {
        return boosters.stream()
                .filter(PlayerBooster::isActive)
                .filter(booster -> booster.matchesConditions(levelGroup, source))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取玩家UUID
     * 
     * @return 玩家UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    /**
     * 清除所有加成
     */
    public void clearBoosters() {
        boosters.clear();
    }
    
    /**
     * 获取加成数量
     * 
     * @return 加成数量
     */
    public int getBoosterCount() {
        return boosters.size();
    }
    
    /**
     * 获取活跃加成数量
     * 
     * @return 活跃加成数量
     */
    public int getActiveBoosterCount() {
        return (int) boosters.stream().filter(PlayerBooster::isActive).count();
    }
    
    /**
     * 获取所有加成（包括过期的）
     * 
     * @return 所有加成列表
     */
    public List<PlayerBooster> getAllBoosters() {
        return new ArrayList<>(boosters);
    }
    
    /**
     * 移除指定加成
     * 
     * @param index 加成索引
     * @return 是否成功移除
     */
    public boolean removeBooster(int index) {
        if (index >= 0 && index < boosters.size()) {
            boosters.remove(index);
            return true;
        }
        return false;
    }
} 
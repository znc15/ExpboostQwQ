package org.littlesheep.expboostQwQ.data;

/**
 * 玩家经验加成数据类
 * 用于存储玩家的经验加成信息，包括倍率、过期时间和限制条件
 */
public class PlayerBooster {
    private final double multiplier;  // 经验加成倍率
    private final long endTime;       // 结束时间戳（毫秒），-1表示永久
    private final String levelGroup;  // 限制的等级组，空字符串表示所有等级组
    private final String source;      // 限制的经验来源，空字符串表示所有来源
    
    /**
     * 构造函数
     * 
     * @param multiplier 经验加成倍率
     * @param endTime 结束时间戳（毫秒），-1表示永久
     * @param levelGroup 限制的等级组，null或空字符串表示所有等级组
     * @param source 限制的经验来源，null或空字符串表示所有来源
     */
    public PlayerBooster(double multiplier, long endTime, String levelGroup, String source) {
        this.multiplier = multiplier;
        this.endTime = endTime;
        this.levelGroup = levelGroup == null ? "" : levelGroup;
        this.source = source == null ? "" : source;
    }
    
    /**
     * 获取经验加成倍率
     * 
     * @return 经验加成倍率
     */
    public double getMultiplier() {
        return multiplier;
    }
    
    /**
     * 获取加成结束时间戳
     * 
     * @return 结束时间戳（毫秒），-1表示永久
     */
    public long getEndTime() {
        return endTime;
    }
    
    /**
     * 获取限制的等级组
     * 
     * @return 限制的等级组名称，空字符串表示所有等级组
     */
    public String getLevelGroup() {
        return levelGroup;
    }
    
    /**
     * 获取限制的经验来源
     * 
     * @return 限制的经验来源名称，空字符串表示所有来源
     */
    public String getSource() {
        return source;
    }
    
    /**
     * 检查加成是否仍然有效
     * 
     * @return 如果加成永久或当前时间小于结束时间，则返回true；否则返回false
     */
    public boolean isActive() {
        return endTime == -1 || endTime > System.currentTimeMillis();
    }
    
    /**
     * 检查指定的等级组和经验来源是否符合此加成的条件
     * 
     * @param levelGroup 要检查的等级组名称
     * @param source 要检查的经验来源名称
     * @return 如果符合条件则返回true，否则返回false
     */
    public boolean matchesConditions(String levelGroup, String source) {
        // 检查等级组匹配（空表示匹配所有）
        boolean levelGroupMatches = this.levelGroup.isEmpty() || this.levelGroup.equals(levelGroup);
        
        // 检查来源匹配（空表示匹配所有）
        boolean sourceMatches = this.source.isEmpty() || this.source.equals(source);
        
        return levelGroupMatches && sourceMatches;
    }
    
    /**
     * 获取加成剩余时间（毫秒）
     * 
     * @return 剩余时间（毫秒），-1表示永久，0表示已过期
     */
    public long getTimeLeft() {
        if (endTime == -1) {
            return -1; // 永久
        }
        
        long timeLeft = endTime - System.currentTimeMillis();
        return Math.max(0, timeLeft);
    }
    
    /**
     * 获取格式化的剩余时间字符串，便于显示
     * 
     * @return 格式化的剩余时间字符串，例如"1天 2小时 3分钟 4秒"
     */
    public String getFormattedTimeLeft() {
        if (endTime == -1) {
            return "永久";
        }
        
        long timeLeft = getTimeLeft();
        if (timeLeft <= 0) {
            return "已过期";
        }
        
        // 转换为天、小时、分钟、秒
        long seconds = timeLeft / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天 ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("小时 ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("分钟 ");
        }
        sb.append(seconds).append("秒");
        
        return sb.toString();
    }
} 
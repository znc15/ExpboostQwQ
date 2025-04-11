package org.littlesheep.expboostQwQ.data;

/**
 * 全服经验加成数据类
 * 继承自PlayerBooster，用于存储全服范围内的经验加成信息
 * 功能与PlayerBooster相同，但应用于所有在线玩家
 */
public class ServerBooster extends PlayerBooster {
    
    /**
     * 构造函数
     * 
     * @param multiplier 经验加成倍率
     * @param endTime 结束时间戳（毫秒），-1表示永久
     * @param levelGroup 限制的等级组，null或空字符串表示所有等级组
     * @param source 限制的经验来源，null或空字符串表示所有来源
     */
    public ServerBooster(double multiplier, long endTime, String levelGroup, String source) {
        super(multiplier, endTime, levelGroup, source);
    }
} 
package org.littlesheep.expboostQwQ.utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间工具类
 * 提供时间格式化和解析功能，用于处理经验加成的持续时间
 */
public class TimeUtils {
    
    // 正则表达式模式：匹配形如"5d"、"3h"、"30m"、"15s"的时间表示
    // 第一个捕获组是数字部分，第二个捕获组是单位部分
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)([dhms])");
    
    /**
     * 解析时间字符串为秒数
     * 支持多种格式组合，例如：
     * - "1d" = 1天 = 86400秒
     * - "2h30m" = 2小时30分钟 = 9000秒
     * - "1d6h30m15s" = 1天6小时30分钟15秒 = 109815秒
     * - "60" = 60秒 (如果没有单位，则为秒)
     * 
     * @param duration 时间字符串
     * @return 解析后的总秒数
     * @throws IllegalArgumentException 如果时间字符串格式无效
     */
    public static long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            throw new IllegalArgumentException("时间字符串不能为空");
        }
        
        Matcher matcher = TIME_PATTERN.matcher(duration.toLowerCase());
        long seconds = 0;
        boolean found = false;
        
        while (matcher.find()) {
            found = true;
            double value = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(3);
            
            switch (unit) {
                case "d":  // 天
                    seconds += TimeUnit.DAYS.toSeconds((long)value) + (long)((value % 1) * 24 * 60 * 60);
                    break;
                case "h":  // 小时
                    seconds += TimeUnit.HOURS.toSeconds((long)value) + (long)((value % 1) * 60 * 60);
                    break;
                case "m":  // 分钟
                    seconds += TimeUnit.MINUTES.toSeconds((long)value) + (long)((value % 1) * 60);
                    break;
                case "s":  // 秒
                    seconds += (long)value;
                    break;
                default:
                    break;
            }
        }
        
        if (!found) {
            try {
                // 尝试将整个字符串解析为秒数
                seconds = Long.parseLong(duration);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的时间格式: " + duration);
            }
        }
        
        return seconds;
    }
    
    /**
     * 将秒数格式化为可读的时间字符串
     * 例如：
     * - 86400秒 = "1天 0小时 0分钟 0秒"
     * - 9000秒 = "2小时 30分钟 0秒"
     * - 70秒 = "1分钟 10秒"
     * 
     * @param seconds 秒数
     * @return 格式化后的时间字符串
     */
    public static String formatDuration(long seconds) {
        if (seconds < 0) {
            return "永久";
        }
        
        // 分解为天、小时、分钟、秒
        long days = TimeUnit.SECONDS.toDays(seconds);
        seconds -= TimeUnit.DAYS.toSeconds(days);
        
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);
        
        // 构建格式化字符串，只显示非零部分
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
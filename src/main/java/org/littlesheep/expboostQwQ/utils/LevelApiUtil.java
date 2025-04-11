package org.littlesheep.expboostQwQ.utils;

import com.github.cpjinan.plugin.akarilevel.api.LevelAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.littlesheep.expboostQwQ.ExpboostQwQ;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * AkariLevel API工具类
 * 封装AkariLevel插件的API调用，简化获取等级组信息
 */
public class LevelApiUtil {
    
    private static boolean isAkariLevelEnabled = false;
    
    /**
     * 初始化工具类
     * @param plugin 插件实例
     */
    public static void init(ExpboostQwQ plugin) {
        try {
            Class.forName("com.github.cpjinan.plugin.akarilevel.api.LevelAPI");
            isAkariLevelEnabled = true;
            LogUtil.info("成功连接到 AkariLevel API");
        } catch (ClassNotFoundException e) {
            isAkariLevelEnabled = false;
            LogUtil.error("未找到 AkariLevel 插件，部分功能可能无法使用");
        }
    }

    /**
     * 获取所有等级组名称列表
     * @return 等级组名称列表
     */
    public static ArrayList<String> getLevelGroupNames() {
        if (!isAkariLevelEnabled) {
            return new ArrayList<>();
        }
        try {
            return LevelAPI.INSTANCE.getLevelGroupNames();
        } catch (Exception e) {
            LogUtil.error("获取等级组列表失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查指定等级组是否存在
     * @param levelGroup 等级组名称
     * @return 是否存在
     */
    public static boolean isLevelGroupExists(String levelGroup) {
        if (!isAkariLevelEnabled) {
            return false;
        }
        try {
            return getLevelGroupNames().contains(levelGroup);
        } catch (Exception e) {
            LogUtil.error("检查等级组是否存在失败", e);
            return false;
        }
    }
    
    /**
     * 获取所有等级组配置
     * @return 等级组配置映射
     */
    public static HashMap<String, ConfigurationSection> getLevelGroups() {
        if (!isAkariLevelEnabled) {
            return new HashMap<>();
        }
        try {
            return LevelAPI.INSTANCE.getLevelGroups();
        } catch (Exception e) {
            LogUtil.error("获取等级组配置失败", e);
            return new HashMap<>();
        }
    }
    
    /**
     * 获取指定等级组升级到某等级所需经验
     * @param levelGroup 等级组编辑名
     * @param level 等级
     * @return 升级所需经验
     */
    public static long getLevelExp(String levelGroup, long level) {
        if (!isAkariLevelEnabled) {
            return 0;
        }
        try {
            return LevelAPI.INSTANCE.getLevelExp(levelGroup, level);
        } catch (Exception e) {
            LogUtil.error("获取等级经验失败", e);
            return 0;
        }
    }
    
    /**
     * 获取指定等级组某等级名称
     * @param levelGroup 等级组编辑名
     * @param level 等级
     * @return 等级名称
     */
    public static String getLevelName(String levelGroup, long level) {
        if (!isAkariLevelEnabled) {
            return "";
        }
        try {
            return LevelAPI.INSTANCE.getLevelName(levelGroup, level);
        } catch (Exception e) {
            LogUtil.error("获取等级名称失败", e);
            return "";
        }
    }
} 
package org.littlesheep.expboostQwQ.listeners;

import com.github.cpjinan.plugin.akarilevel.common.event.exp.PlayerExpChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.littlesheep.expboostQwQ.ExpboostQwQ;
import org.littlesheep.expboostQwQ.data.BoosterManager;
import org.littlesheep.expboostQwQ.utils.LogUtil;

/**
 * AkariLevel经验获取事件监听器
 * 用于在玩家获得经验时应用经验倍率加成
 */
public class ExpGainListener implements Listener {
    
    // 插件实例引用
    private final ExpboostQwQ plugin;
    // 加成管理器引用
    private final BoosterManager boosterManager;
    
    /**
     * 构造函数
     * @param plugin 插件主类实例
     */
    public ExpGainListener(ExpboostQwQ plugin) {
        this.plugin = plugin;
        this.boosterManager = plugin.getBoosterManager();
    }
    
    /**
     * 监听AkariLevel的经验变更事件
     * 事件优先级设为NORMAL，确保在其他插件处理之后，但在最终处理之前执行
     * 
     * @param event 玩家经验变更事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onExpGain(PlayerExpChangeEvent event) {
        // 从事件中获取相关信息
        Player player = event.getPlayer();        // 获取玩家对象
        String levelGroup = event.getLevelGroup(); // 获取等级组名称
        String source = event.getSource();         // 获取经验来源
        int expAmount = (int) event.getExpAmount();      // 获取经验变化量
        
        // 如果经验值不是正数，说明是扣除经验，不进行加成处理
        if (expAmount <= 0) {
            return;
        }
        
        // 调试日志
        LogUtil.debug("玩家 " + player.getName() + " 在等级组 " + levelGroup + 
                " 从来源 " + source + " 获得了 " + expAmount + " 点经验");
        
        // 检查玩家在此等级组和经验来源下是否有经验加成
        // 这会同时检查玩家个人加成、全服加成和默认倍率
        double multiplier = boosterManager.getEffectiveMultiplier(player, levelGroup, source);
        
        // 如果倍率不大于1.0，说明没有加成效果，直接返回不处理
        if (multiplier <= 1.0) {
            return;
        }
        
        // 计算应用加成后的新经验值
        // 使用Math.round确保结果为整数并正确四舍五入
        int newExpAmount = (int)Math.round(expAmount * multiplier);
        
        // 将新的经验值设置回事件对象，这会影响最终玩家获得的经验值
        event.setExpAmount(newExpAmount);
        
        // 记录日志
        LogUtil.debug("玩家 " + player.getName() + " 经验已加成 " + expAmount + 
                " → " + newExpAmount + " (×" + multiplier + ")");
        
        // 如果在配置中启用了经验加成消息提示，且没有开启全局静默模式
        if (plugin.getConfig().getBoolean("settings.show_exp_boost_message", true) && 
            !plugin.getConfig().getBoolean("settings.silent_mode", false)) {
            
            // 获取玩家语言
            String langCode = plugin.getLanguageManager().getPlayerLanguage(player.getUniqueId().toString());
            
            // 从语言文件中获取消息模板
            String message = plugin.getLanguageManager().getMessage(
                    langCode,
                    "messages.exp_boost.exp_boosted", 
                    "§a[ExpboostQwQ] §f获得经验 §e%original% §f→ §e%boosted% §7(×%multiplier%)")
                    .replace("%original%", String.valueOf(expAmount))     // 替换原始经验值
                    .replace("%boosted%", String.valueOf(newExpAmount))   // 替换加成后经验值
                    .replace("%multiplier%", String.format("%.1f", multiplier)); // 替换倍率，保留一位小数
            
            // 向玩家发送经验加成消息
            player.sendMessage(message);
        }
    }
} 
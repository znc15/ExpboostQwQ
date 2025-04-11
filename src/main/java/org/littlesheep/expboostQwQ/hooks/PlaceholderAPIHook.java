package org.littlesheep.expboostQwQ.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.littlesheep.expboostQwQ.ExpboostQwQ;
import org.littlesheep.expboostQwQ.data.PlayerBooster;
import org.littlesheep.expboostQwQ.data.ServerBooster;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final ExpboostQwQ plugin;

    public PlaceholderAPIHook(ExpboostQwQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "expboostqwq";
    }

    @Override
    public String getAuthor() {
        return "LittleSheep";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        switch (identifier.toLowerCase()) {
            case "player_multiplier":
                PlayerBooster pb = plugin.getBoosterManager().getPlayerBooster(player.getUniqueId());
                return pb != null && pb.isActive() ? String.format("%.2f", pb.getMultiplier()) : "1.00";
            
            case "player_duration":
                pb = plugin.getBoosterManager().getPlayerBooster(player.getUniqueId());
                if (pb != null && pb.isActive()) {
                    return pb.getEndTime() == -1 ? "永久" : String.valueOf(pb.getTimeLeft() / 1000);
                }
                return "0";
            
            case "server_multiplier":
                ServerBooster sb = plugin.getBoosterManager().getServerBooster();
                return sb != null && sb.isActive() ? String.format("%.2f", sb.getMultiplier()) : "1.00";
            
            case "server_duration":
                sb = plugin.getBoosterManager().getServerBooster();
                if (sb != null && sb.isActive()) {
                    return sb.getEndTime() == -1 ? "永久" : String.valueOf(sb.getTimeLeft() / 1000);
                }
                return "0";
            
            case "total_multiplier":
                return String.format("%.2f", plugin.getBoosterManager().getEffectiveMultiplier(player, "", ""));
            
            default:
                return null;
        }
    }
} 
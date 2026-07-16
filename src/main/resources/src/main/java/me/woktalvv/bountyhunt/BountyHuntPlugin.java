package me.woktalvv.bountyhunt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BountyHuntPlugin extends JavaPlugin {

    private final Map<UUID, BountyHunt> activeHunts = new ConcurrentHashMap<>();
    private FileConfiguration config;
    private BossBar bossBar;
    private int prepTime;
    private double huntTimeMultiplier;
    private boolean deathPenalty;
    private boolean lootReward;
    private String bossBarTitle;
    private BarColor bossBarColor;
    private BarStyle bossBarStyle;
    private Map<String, String> messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        getLogger().info("BountyHunt plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BountyHunt plugin disabled!");
    }

    private void loadConfig() {
        config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
        prepTime = config.getInt("prep-time", 900);
        huntTimeMultiplier = config.getDouble("hunt-time-multiplier", 0.1);
        deathPenalty = config.getBoolean("death-penalty", true);
        lootReward = config.getBoolean("loot-reward", true);

        bossBarTitle = ChatColor.translateAlternateColorCodes('&',
            config.getString("bossbar.title", "&cDistance to Target: {distance}m"));
        bossBarColor = BarColor.valueOf(config.getString("bossbar.color", "RED").toUpperCase());
        bossBarStyle = BarStyle.valueOf(config.getString("bossbar.style", "SEGMENTED_10").toUpperCase());

        messages = new HashMap<>();
        for (String key : config.getConfigurationSection("messages").getKeys(false)) {
            messages.put(key, ChatColor.translateAlternateColorCodes('&',
                config.getString("messages." + key, "")));
        }
    }

    public BossBar createBossBar(Player hunter, Player target) {
        if (bossBar != null) bossBar.removeAll();
        double distance = hunter.getLocation().distance(target.getLocation());
        bossBar = Bukkit.createBossBar(
            bossBarTitle.replace("{distance}", String.format("%.0f", distance)),
            bossBarColor,
            bossBarStyle
        );
        bossBar.addPlayer(hunter);
        bossBar.setProgress(Math.min(1.0, distance / 1000.0));
        return bossBar;
    }

    public void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public Map<UUID, BountyHunt> getActiveHunts() { return activeHunts; }
    public int getPrepTime() { return prepTime; }
    public double getHuntTimeMultiplier() { return huntTimeMultiplier; }
    public boolean isDeathPenalty() { return deathPenalty; }
    public boolean isLootReward() { return lootReward; }
    public String getMessage(String key) { return messages.getOrDefault(key, "&cMessage not found: " + key); }

    public void startHunt(Player hunter) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.remove(hunter);
        if (onlinePlayers.isEmpty()) {
            hunter.sendMessage(getMessage("not-enough-players"));
            return;
        }
        if (activeHunts.containsKey(hunter.getUniqueId())) {
            hunter.sendMessage(getMessage("already-hunting"));
            return;
        }
        Player target = onlinePlayers.get(new Random().nextInt(onlinePlayers.size()));
        BountyHunt hunt = new BountyHunt(this, hunter, target);
        activeHunts.put(hunter.getUniqueId(), hunt);
        hunt.startPreparationTimer();
        hunter.sendMessage(getMessage("hunt-started").replace("{prepTime}", String.valueOf(prepTime / 60)));
        String prepTimeMessage = getMessage("target-notified")
            .replace("{hunter}", hunter.getName())
            .replace("{prepTime}", String.valueOf(prepTime / 60));
        target.sendMessage(prepTimeMessage);
    }

    public void cancelHunt(Player hunter) {
        BountyHunt hunt = activeHunts.remove(hunter.getUniqueId());
        if (hunt != null) {
            hunt.cancel();
            hunter.sendMessage(getMessage("hunt-cancelled"));
            if (hunt.getTarget() != null) hunt.getTarget().sendMessage(getMessage("hunt-cancelled"));
        } else hunter.sendMessage(getMessage("not-hunting"));
    }

    public void listHunts(Player player) {
        if (activeHunts.isEmpty()) {
            player.sendMessage(getMessage("no-active-hunts"));
            return;
        }
        player.sendMessage(ChatColor.GOLD + "Active Bounty Hunts:");
        for (BountyHunt hunt : activeHunts.values()) {
            player.sendMessage(ChatColor.YELLOW + "- " + hunt.getHunter().getName() + " -> " + hunt.getTarget().getName());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        if (!cmd.getName().equalsIgnoreCase("bounty")) return false;
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /bounty start|cancel|list");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "start": startHunt(player); break;
            case "cancel": cancelHunt(player); break;
            case "list": listHunts(player); break;
            default: player.sendMessage(ChatColor.RED + "Unknown subcommand. Use: start, cancel, or list.");
        }
        return true;
    }
}

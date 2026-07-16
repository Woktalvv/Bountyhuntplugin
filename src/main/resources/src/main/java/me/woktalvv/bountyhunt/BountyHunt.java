package me.woktalvv.bountyhunt;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BountyHunt {

    private final BountyHuntPlugin plugin;
    private final Player hunter;
    private final Player target;
    private int prepTimeRemaining;
    private int huntTimeRemaining;
    private boolean isActive;
    private BukkitRunnable prepTask;
    private BukkitRunnable huntTask;

    public BountyHunt(BountyHuntPlugin plugin, Player hunter, Player target) {
        this.plugin = plugin;
        this.hunter = hunter;
        this.target = target;
        this.prepTimeRemaining = plugin.getPrepTime();
        this.isActive = true;
    }

    public Player getHunter() { return hunter; }
    public Player getTarget() { return target; }

    public void startPreparationTimer() {
        prepTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive) { cancel(); return; }
                if (!hunter.isOnline() || !target.isOnline()) return;
                prepTimeRemaining--;
                if (prepTimeRemaining <= 0) {
                    startHuntTimer();
                    cancel();
                }
            }
        };
        prepTask.runTaskTimer(plugin, 20L, 20L);
    }

    private void startHuntTimer() {
        if (!isActive) return;
        double distance = hunter.getLocation().distance(target.getLocation());
        huntTimeRemaining = (int) (distance * plugin.getHuntTimeMultiplier());
        plugin.createBossBar(hunter, target);
        hunter.sendMessage(plugin.getMessage("hunt-time-up"));

        huntTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive) { cancel(); return; }
                if (!hunter.isOnline() || !target.isOnline()) return;
                if (target.isDead()) {
                    completeHunt(true);
                    cancel();
                    return;
                }
                double currentDistance = hunter.getLocation().distance(target.getLocation());
                plugin.createBossBar(hunter, target);
                huntTimeRemaining--;
                if (huntTimeRemaining <= 0) {
                    completeHunt(false);
                    cancel();
                }
            }
        };
        huntTask.runTaskTimer(plugin, 20L, 20L);
    }

    public void completeHunt(boolean success) {
        isActive = false;
        plugin.removeBossBar();
        if (success) {
            if (plugin.isLootReward()) {
                hunter.sendMessage(plugin.getMessage("hunter-won"));
                target.sendMessage(plugin.getMessage("target-killed"));
            }
        } else {
            if (plugin.isDeathPenalty()) {
                hunter.setHealth(0);
                hunter.sendMessage(plugin.getMessage("hunter-failed"));
            }
        }
        plugin.getActiveHunts().remove(hunter.getUniqueId());
    }

    public void cancel() {
        isActive = false;
        if (prepTask != null) prepTask.cancel();
        if (huntTask != null) huntTask.cancel();
        plugin.removeBossBar();
    }
}

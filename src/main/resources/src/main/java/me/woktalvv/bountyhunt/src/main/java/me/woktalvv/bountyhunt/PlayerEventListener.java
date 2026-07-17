package me.woktalvv.bountyhunt;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEventListener implements Listener {

    private final BountyHuntPlugin plugin;

    public PlayerEventListener(BountyHuntPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BountyHunt hunt = plugin.getActiveHunts().get(player.getUniqueId());
        if (hunt != null && hunt.getTarget() != null && hunt.getTarget().isOnline()) {
            plugin.createBossBar(player, hunt.getTarget());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        if (killer != null) {
            BountyHunt hunt = plugin.getActiveHunts().get(killer.getUniqueId());
            if (hunt != null && hunt.getTarget().equals(player)) {
                hunt.completeHunt(true);
            }
        }
        BountyHunt huntersHunt = plugin.getActiveHunts().get(player.getUniqueId());
        if (huntersHunt != null) {
            huntersHunt.cancel();
            plugin.getActiveHunts().remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        BountyHunt hunt = plugin.getActiveHunts().get(player.getUniqueId());
        if (hunt != null) {
            hunt.cancel();
            plugin.getActiveHunts().remove(player.getUniqueId());
        }
    }
}

package com.example.voidcatcher.util;

import com.example.voidcatcher.model.VoidRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RegionVisualizer {
    public static void show(JavaPlugin plugin, Player player, VoidRegion r, int seconds) {
        World w = Bukkit.getWorld(r.worldName);
        if (w == null) return;

        int minX = r.getMinX();
        int maxX = r.getMaxX();
        int minZ = r.getMinZ();
        int maxZ = r.getMaxZ();
        int y = Math.max(player.getLocation().getBlockY(), r.fallY + 2);

        int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (int x = minX; x <= maxX; x += 1) {
                w.spawnParticle(Particle.HAPPY_VILLAGER, new Location(w, x + 0.5, y, minZ + 0.5), 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.HAPPY_VILLAGER, new Location(w, x + 0.5, y, maxZ + 0.5), 1, 0, 0, 0, 0);
            }
            for (int z = minZ; z <= maxZ; z += 1) {
                w.spawnParticle(Particle.HAPPY_VILLAGER, new Location(w, minX + 0.5, y, z + 0.5), 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.HAPPY_VILLAGER, new Location(w, maxX + 0.5, y, z + 0.5), 1, 0, 0, 0, 0);
            }
        }, 0L, 10L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.getScheduler().cancelTask(task), seconds * 20L);
    }
}

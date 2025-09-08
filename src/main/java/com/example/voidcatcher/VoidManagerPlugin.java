package com.example.voidcatcher;

import com.example.voidcatcher.commands.VoidCommand;
import com.example.voidcatcher.model.VoidRegion;
import com.example.voidcatcher.model.VoidRegion.MessageMode;
import com.example.voidcatcher.util.Messages;
import com.example.voidcatcher.util.Yaml;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class VoidManagerPlugin extends JavaPlugin implements Listener {

    private final Map<String, VoidRegion> regions = new ConcurrentHashMap<>(); // key = name (unique)
    private final Map<UUID, long[]> lastTp = new ConcurrentHashMap<>(); // per-player last tp time per region hash
    private final Set<UUID> bypass = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, int[]> pos1 = new ConcurrentHashMap<>(); // x,z,y cached for feedback
    private final Map<UUID, int[]> pos2 = new ConcurrentHashMap<>();

    private int taskId = -1;
    private int cooldownSeconds;
    private boolean showParticles;
    private int particleSeconds;

    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        messages = new Messages(this);

        reloadSettings();
        loadRegions();

        // Task every 10 ticks
        taskId = Bukkit.getScheduler().runTaskTimer(this, this::tickPlayers, 10L, 10L).getTaskId();

        // Command
        VoidCommand command = new VoidCommand(this);
        Objects.requireNonNull(getCommand("void")).setExecutor(command);
        Objects.requireNonNull(getCommand("void")).setTabCompleter(command);

        getLogger().info("VoidManager enabled. Regions: " + regions.size());
    }

    @Override
    public void onDisable() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        getLogger().info("VoidManager disabled.");
    }

    public void reloadAll() {
        reloadConfig();
        messages.reload();
        reloadSettings();
        regions.clear();
        loadRegions();
    }

    private void reloadSettings() {
        FileConfiguration cfg = getConfig();
        cooldownSeconds = cfg.getInt("cooldown-seconds", 3);
        showParticles = cfg.getBoolean("show-particles", true);
        particleSeconds = cfg.getInt("particle-seconds", 2);
    }

    private void saveResourceIfMissing(String name) {
        File f = new File(getDataFolder(), name);
        if (!f.exists()) saveResource(name, false);
    }

    private void loadRegions() {
        regions.clear();
        FileConfiguration cfg = getConfig();
        ConfigurationSection worlds = cfg.getConfigurationSection("regions");
        if (worlds == null) return;

        for (String worldName : worlds.getKeys(false)) {
            ConfigurationSection wSec = worlds.getConfigurationSection(worldName);
            if (wSec == null) continue;

            for (String rName : wSec.getKeys(false)) {
                ConfigurationSection rSec = wSec.getConfigurationSection(rName);
                if (rSec == null) continue;

                boolean enabled = rSec.getBoolean("enabled", true);
                int priority = rSec.getInt("priority", 0);
                int fromX = rSec.getConfigurationSection("pos1").getInt("x");
                int fromZ = rSec.getConfigurationSection("pos1").getInt("z");
                int toX = rSec.getConfigurationSection("pos2").getInt("x");
                int toZ = rSec.getConfigurationSection("pos2").getInt("z");
                int fallY = rSec.getInt("fallY", 0);

                ConfigurationSection tp = rSec.getConfigurationSection("tp");
                String tpWorld = (tp != null) ? tp.getString("world", worldName) : worldName;
                int tpX = (tp != null) ? tp.getInt("x", 0) : 0;
                int tpY = (tp != null) ? tp.getInt("y", 64) : 64;
                int tpZ = (tp != null) ? tp.getInt("z", 0) : 0;
                float yaw = (float) (tp != null ? tp.getDouble("yaw", 0d) : 0);
                float pitch = (float) (tp != null ? tp.getDouble("pitch", 0d) : 0);

                ConfigurationSection msg = rSec.getConfigurationSection("message");
                String text = (msg != null) ? msg.getString("text", "") : "";
                String modeS = (msg != null) ? msg.getString("mode", "chat") : "chat";
                VoidRegion.MessageMode mode;
                try {
                    mode = MessageMode.valueOf(modeS.toUpperCase(Locale.ROOT));
                } catch (Exception e) {
                    mode = MessageMode.CHAT;
                }

                String sound = rSec.getString("sound", null);

                VoidRegion region = new VoidRegion(
                        rName, worldName,
                        fromX, fromZ, toX, toZ,
                        fallY, tpX, tpY, tpZ, yaw, pitch,
                        enabled, priority,
                        text, mode, sound
                );
                regions.put(rName, region);
            }
        }
    }

    public void saveRegion(VoidRegion r) {
        FileConfiguration cfg = getConfig();
        ConfigurationSection worlds = cfg.getConfigurationSection("regions");
        if (worlds == null) worlds = cfg.createSection("regions");
        ConfigurationSection wSec = worlds.getConfigurationSection(r.worldName);
        if (wSec == null) wSec = worlds.createSection(r.worldName);
        ConfigurationSection rSec = wSec.getConfigurationSection(r.name);
        if (rSec == null) rSec = wSec.createSection(r.name);

        rSec.set("enabled", r.enabled);
        rSec.set("priority", r.priority);

        ConfigurationSection p1 = rSec.getConfigurationSection("pos1");
        if (p1 == null) p1 = rSec.createSection("pos1");
        p1.set("x", r.fromX);
        p1.set("z", r.fromZ);

        ConfigurationSection p2 = rSec.getConfigurationSection("pos2");
        if (p2 == null) p2 = rSec.createSection("pos2");
        p2.set("x", r.toX);
        p2.set("z", r.toZ);

        rSec.set("fallY", r.fallY);

        ConfigurationSection tp = rSec.getConfigurationSection("tp");
        if (tp == null) tp = rSec.createSection("tp");
        tp.set("world", r.worldName);
        tp.set("x", r.tpX);
        tp.set("y", r.tpY);
        tp.set("z", r.tpZ);
        tp.set("yaw", r.tpYaw);
        tp.set("pitch", r.tpPitch);

        ConfigurationSection msg = rSec.getConfigurationSection("message");
        if (msg == null) msg = rSec.createSection("message");
        msg.set("mode", r.messageMode.name().toLowerCase(Locale.ROOT));
        msg.set("text", r.messageText == null ? "" : r.messageText);

        rSec.set("sound", r.sound);

        saveConfig();
    }

    public void deleteRegion(String name) {
        VoidRegion r = regions.remove(name);
        if (r == null) return;
        FileConfiguration cfg = getConfig();
        ConfigurationSection worlds = cfg.getConfigurationSection("regions");
        if (worlds == null) return;
        ConfigurationSection wSec = worlds.getConfigurationSection(r.worldName);
        if (wSec == null) return;
        wSec.set(name, null);
        saveConfig();
    }

    private void tickPlayers() {
        if (regions.isEmpty()) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (bypass.contains(p.getUniqueId())) continue;

            Location loc = p.getLocation();
            String worldName = loc.getWorld().getName();
            int px = loc.getBlockX();
            int py = loc.getBlockY();
            int pz = loc.getBlockZ();

            // Find highest priority matching region in this world
            VoidRegion best = null;
            for (VoidRegion r : regions.values()) {
                if (!r.enabled || !r.worldName.equals(worldName)) continue;
                if (!r.containsXZ(px, pz)) continue;
                if (best == null || r.priority > best.priority) best = r;
            }
            if (best == null) continue;
            if (py > best.fallY) continue;

            // Cooldown
            String key = best.worldName + ":" + best.name;
            long hash = key.hashCode();
            long now = System.currentTimeMillis();
            long until = getCooldownUntil(p.getUniqueId(), hash);
            if (now < until) continue;

            // Teleport
            World w = Bukkit.getWorld(best.worldName);
            if (w == null) continue;
            Location tp = new Location(w, best.tpX + 0.5, best.tpY, best.tpZ + 0.5, best.tpYaw, best.tpPitch);
            p.teleport(tp);

            // Message
            messages.sendMode(p, best, "§cYou fell into the void and were teleported!");

            // Sound
            if (best.sound != null && !best.sound.isBlank()) {
                try {
                    p.playSound(tp, Sound.valueOf(best.sound), 1f, 1f);
                } catch (IllegalArgumentException ignored) {}
            }

            setCooldown(p.getUniqueId(), hash, now + cooldownSeconds * 1000L);
        }
    }

    private long getCooldownUntil(UUID id, long regionKey) {
        long[] arr = lastTp.computeIfAbsent(id, k -> new long[1]);
        return arr[0];
    }

    private void setCooldown(UUID id, long regionKey, long until) {
        long[] arr = lastTp.computeIfAbsent(id, k -> new long[1]);
        arr[0] = until;
    }

    // --- Accessors & helpers used by command class ---

    public Map<String, VoidRegion> getRegions() { return regions; }
    public Messages getMessages() { return messages; }
    public boolean isShowParticles() { return showParticles; }
    public int getParticleSeconds() { return particleSeconds; }

    public void setPos1(UUID id, int x, int y, int z) { pos1.put(id, new int[]{x,y,z}); }
    public void setPos2(UUID id, int x, int y, int z) { pos2.put(id, new int[]{x,y,z}); }
    public int[] getPos1(UUID id) { return pos1.get(id); }
    public int[] getPos2(UUID id) { return pos2.get(id); }

    public void toggleBypass(UUID id, Player p) {
        if (bypass.contains(id)) {
            bypass.remove(id);
            p.sendMessage(messages.comp(messages.prefixed("bypass.disabled", "&cBypass disabled.")));
        } else {
            bypass.add(id);
            p.sendMessage(messages.comp(messages.prefixed("bypass.enabled", "&aBypass enabled.")));
        }
    }
}

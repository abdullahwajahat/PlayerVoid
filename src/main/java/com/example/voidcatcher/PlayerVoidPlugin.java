package com.example.voidcatcher;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerVoidPlugin extends JavaPlugin {

    private final Map<String, VoidRegion> regions = new ConcurrentHashMap<>();
    private int taskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadRegionsFromConfig();

        // schedule check every 10 ticks (~0.5s)
        taskId = Bukkit.getScheduler().runTaskTimer(this, this::checkPlayers, 10L, 10L).getTaskId();

        getLogger().info("PlayerVoidPlugin enabled. Regions loaded: " + regions.size());
    }

    @Override
    public void onDisable() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        getLogger().info("PlayerVoidPlugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("voidset")) return false;

        if (!sender.isOp() && !sender.hasPermission("playervoid.set")) {
            sender.sendMessage("§cYou need to be OP or have permission playervoid.set");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        try {
            switch (sub) {
                case "create":
                    if (args.length != 10) {
                        sender.sendMessage("§cUsage: /voidset create <name> <fromX> <fromZ> <toX> <toZ> <fallY> <tpX> <tpY> <tpZ>");
                        return true;
                    }
                    handleCreate(sender, args);
                    break;

                case "remove":
                    if (args.length != 2) {
                        sender.sendMessage("§cUsage: /voidset remove <name>");
                        return true;
                    }
                    handleRemove(sender, args[1]);
                    break;

                case "list":
                    handleList(sender);
                    break;

                case "info":
                    if (args.length != 2) {
                        sender.sendMessage("§cUsage: /voidset info <name>");
                        return true;
                    }
                    handleInfo(sender, args[1]);
                    break;

                default:
                    sendUsage(sender);
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cAll numeric arguments must be integers.");
        } catch (Exception ex) {
            sender.sendMessage("§cError: " + ex.getMessage());
            getLogger().warning("Error in voidset command: " + ex);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6VoidSet Usage:");
        sender.sendMessage("§e/voidset create <name> <fromX> <fromZ> <toX> <toZ> <fallY> <tpX> <tpY> <tpZ>");
        sender.sendMessage("§e/voidset remove <name>");
        sender.sendMessage("§e/voidset list");
        sender.sendMessage("§e/voidset info <name>");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        String name = args[1];
        if (regions.containsKey(name)) {
            sender.sendMessage("§cA region with that name already exists. Use a different name.");
            return;
        }

        int fromX = Integer.parseInt(args[2]);
        int fromZ = Integer.parseInt(args[3]);
        int toX = Integer.parseInt(args[4]);
        int toZ = Integer.parseInt(args[5]);
        int fallY = Integer.parseInt(args[6]);
        int tpX = Integer.parseInt(args[7]);
        int tpY = Integer.parseInt(args[8]);
        int tpZ = Integer.parseInt(args[9]);

        World world = Bukkit.getWorlds().get(0); // default world
        VoidRegion region = new VoidRegion(name, world.getName(), fromX, fromZ, toX, toZ, fallY, tpX, tpY, tpZ, true);
        regions.put(name, region);
        saveRegionToConfig(region);

        sender.sendMessage("§aRegion " + name + " created and saved.");
        getLogger().info("Created region " + name);
    }

    private void handleRemove(CommandSender sender, String name) {
        if (!regions.containsKey(name)) {
            sender.sendMessage("§cNo region named '" + name + "' found.");
            return;
        }
        regions.remove(name);
        removeRegionFromConfig(name);
        sender.sendMessage("§aRegion " + name + " removed.");
        getLogger().info("Removed region " + name);
    }

    private void handleList(CommandSender sender) {
        if (regions.isEmpty()) {
            sender.sendMessage("§eNo regions defined.");
            return;
        }
        sender.sendMessage("§6Defined void regions:");
        for (VoidRegion r : regions.values()) {
            sender.sendMessage(" §e- " + r.name + " (world=" + r.worldName + ")");
        }
    }

    private void handleInfo(CommandSender sender, String name) {
        VoidRegion r = regions.get(name);
        if (r == null) {
            sender.sendMessage("§cNo region named '" + name + "' found.");
            return;
        }
        sender.sendMessage("§6Region: §e" + name);
        sender.sendMessage(" §fWorld: §e" + r.worldName);
        sender.sendMessage(" §fFromX: §e" + r.getMinX() + " §fToX: §e" + r.getMaxX());
        sender.sendMessage(" §fFromZ: §e" + r.getMinZ() + " §fToZ: §e" + r.getMaxZ());
        sender.sendMessage(" §fFallY: §e" + r.fallY);
        sender.sendMessage(" §fTP: §e" + r.tpX + "," + r.tpY + "," + r.tpZ);
        sender.sendMessage(" §fEnabled: §e" + r.enabled);
    }

    private void checkPlayers() {
    if (regions.isEmpty()) return;

    Collection<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
    for (Player p : online) {
        Location loc = p.getLocation();
        int px = loc.getBlockX();
        int py = loc.getBlockY();
        int pz = loc.getBlockZ();
        String worldName = p.getWorld().getName();

        for (VoidRegion r : regions.values()) {
            if (!r.enabled) continue;
            if (!r.worldName.equals(worldName)) continue;

            if (px >= r.getMinX() && px <= r.getMaxX()
                    && pz >= r.getMinZ() && pz <= r.getMaxZ()
                    && py <= r.fallY) {

                World world = Bukkit.getWorld(r.worldName);
                if (world == null) continue;
                Location tp = new Location(world, r.tpX + 0.5, r.tpY, r.tpZ + 0.5);
                p.teleport(tp);
                p.sendMessage("§cYou fell into the void and were teleported!");
                break;
            }
        }
    }


    }

    private void loadRegionsFromConfig() {
        FileConfiguration cfg = getConfig();
        ConfigurationSection section = cfg.getConfigurationSection("regions");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection rsec = section.getConfigurationSection(key);
            if (rsec == null) continue;
            String worldName = rsec.getString("world", Bukkit.getWorlds().get(0).getName());
            int fromX = rsec.getInt("fromX");
            int fromZ = rsec.getInt("fromZ");
            int toX = rsec.getInt("toX");
            int toZ = rsec.getInt("toZ");
            int fallY = rsec.getInt("fallY");
            int tpX = rsec.getInt("tpX");
            int tpY = rsec.getInt("tpY");
            int tpZ = rsec.getInt("tpZ");
            boolean enabled = rsec.getBoolean("enabled", true);

            VoidRegion r = new VoidRegion(key, worldName, fromX, fromZ, toX, toZ, fallY, tpX, tpY, tpZ, enabled);
            regions.put(key, r);
        }
    }

    private void saveRegionToConfig(VoidRegion r) {
        FileConfiguration cfg = getConfig();
        ConfigurationSection section = cfg.getConfigurationSection("regions");
        if (section == null) section = cfg.createSection("regions");
        ConfigurationSection rsec = section.createSection(r.name);
        rsec.set("world", r.worldName);
        rsec.set("fromX", r.fromX);
        rsec.set("fromZ", r.fromZ);
        rsec.set("toX", r.toX);
        rsec.set("toZ", r.toZ);
        rsec.set("fallY", r.fallY);
        rsec.set("tpX", r.tpX);
        rsec.set("tpY", r.tpY);
        rsec.set("tpZ", r.tpZ);
        rsec.set("enabled", r.enabled);
        saveConfig();
    }

    private void removeRegionFromConfig(String name) {
        FileConfiguration cfg = getConfig();
        ConfigurationSection section = cfg.getConfigurationSection("regions");
        if (section != null) {
            section.set(name, null);
            saveConfig();
        }
    }

    private static class VoidRegion {
        final String name;
        final String worldName;
        final int fromX, fromZ, toX, toZ;
        final int fallY;
        final int tpX, tpY, tpZ;
        boolean enabled;

        VoidRegion(String name, String worldName, int fromX, int fromZ, int toX, int toZ, int fallY, int tpX, int tpY, int tpZ, boolean enabled) {
            this.name = name;
            this.worldName = worldName;
            this.fromX = fromX;
            this.fromZ = fromZ;
            this.toX = toX;
            this.toZ = toZ;
            this.fallY = fallY;
            this.tpX = tpX;
            this.tpY = tpY;
            this.tpZ = tpZ;
            this.enabled = enabled;
        }

        int getMinX() { return Math.min(fromX, toX); }
        int getMaxX() { return Math.max(fromX, toX); }
        int getMinZ() { return Math.min(fromZ, toZ); }
        int getMaxZ() { return Math.max(fromZ, toZ); }
    }
}


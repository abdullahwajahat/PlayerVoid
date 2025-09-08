package com.example.voidcatcher;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerVoidPlugin extends JavaPlugin implements TabCompleter {

    private final Map<String, VoidRegion> regions = new ConcurrentHashMap<>();
    private int taskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadRegionsFromConfig();

        // schedule check every 10 ticks (~0.5s)
        taskId = Bukkit.getScheduler().runTaskTimer(this, this::checkPlayers, 10L, 10L).getTaskId();

        PluginCommand cmd = getCommand("voidset");
        if (cmd != null) cmd.setTabCompleter(this);

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
                    if (args.length != 11) {
                        sender.sendMessage("§cUsage: /voidset create <name> <fromX> <fromZ> <toX> <toZ> <fallY> <tpX> <tpY> <tpZ> <message>");
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

                case "rename":
                    if (args.length != 3) {
                        sender.sendMessage("§cUsage: /voidset rename <oldName> <newName>");
                        return true;
                    }
                    handleRename(sender, args[1], args[2]);
                    break;

                case "edit":
                    if (args.length < 4) {
                        sender.sendMessage("§cUsage: /voidset edit <region> <field> <value> [<field> <value> ...]");
                        return true;
                    }
                    handleEdit(sender, args);
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
        sender.sendMessage("§e/voidset create <name> <fromX> <fromZ> <toX> <toZ> <fallY> <tpX> <tpY> <tpZ> <message>");
        sender.sendMessage("§e/voidset remove <name>");
        sender.sendMessage("§e/voidset list");
        sender.sendMessage("§e/voidset info <name>");
        sender.sendMessage("§e/voidset rename <oldName> <newName>");
        sender.sendMessage("§e/voidset edit <region> <field> <value> [<field> <value> ...]");
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
        String message = args[10].replace("_", " ");

        World world = Bukkit.getWorlds().get(0); // default world
        VoidRegion region = new VoidRegion(name, world.getName(), fromX, fromZ, toX, toZ, fallY, tpX, tpY, tpZ, message, true);
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
        sender.sendMessage(" §fMessage: §e" + r.message);
        sender.sendMessage(" §fEnabled: §e" + r.enabled);
    }

    private void handleRename(CommandSender sender, String oldName, String newName) {
        VoidRegion r = regions.remove(oldName);
        if (r == null) {
            sender.sendMessage("§cNo region named '" + oldName + "' found.");
            return;
        }
        r.name = newName;
        regions.put(newName, r);
        removeRegionFromConfig(oldName);
        saveRegionToConfig(r);
        sender.sendMessage("§aRegion " + oldName + " renamed to " + newName);
    }

    private void handleEdit(CommandSender sender, String[] args) {
        String name = args[1];
        VoidRegion r = regions.get(name);
        if (r == null) {
            sender.sendMessage("§cNo region named '" + name + "' found.");
            return;
        }

        Map<String, String> updates = new HashMap<>();
        for (int i = 2; i < args.length - 1; i += 2) {
            String field = args[i].toLowerCase();
            String value = args[i + 1];
            updates.put(field, value);
        }

        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue();

            try {
                switch (field) {
                    case "fromx": r.fromX = Integer.parseInt(value); break;
                    case "fromz": r.fromZ = Integer.parseInt(value); break;
                    case "tox":   r.toX = Integer.parseInt(value); break;
                    case "toz":   r.toZ = Integer.parseInt(value); break;
                    case "fally": r.fallY = Integer.parseInt(value); break;
                    case "tpx":   r.tpX = Integer.parseInt(value); break;
                    case "tpy":   r.tpY = Integer.parseInt(value); break;
                    case "tpz":   r.tpZ = Integer.parseInt(value); break;
                    case "message": r.message = value.replace("_", " "); break;
                    case "enabled": r.enabled = Boolean.parseBoolean(value); break;
                    default:
                        sender.sendMessage("§cUnknown field: " + field);
                        continue;
                }
                sender.sendMessage("§aUpdated " + field + " = " + value);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cField '" + field + "' requires a number, but got: " + value);
            }
        }

        saveRegionToConfig(r);
        sender.sendMessage("§aRegion " + name + " updated.");
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
                    p.sendMessage(r.message.replace("%player%", p.getName()));
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
            String message = rsec.getString("message", "§cYou fell into the void and were teleported!");
            boolean enabled = rsec.getBoolean("enabled", true);

            VoidRegion r = new VoidRegion(key, worldName, fromX, fromZ, toX, toZ, fallY, tpX, tpY, tpZ, message, enabled);
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
        rsec.set("message", r.message);
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

    // ---------------- Tab Completion ----------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("voidset")) return null;

        if (args.length == 1) {
            return Arrays.asList("create", "remove", "list", "info", "rename", "edit");
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "remove":
                case "info":
                case "rename":
                case "edit":
                    return new ArrayList<>(regions.keySet());
            }
        }

        if (args[0].equalsIgnoreCase("create")) {
            switch (args.length) {
                case 2: return Collections.singletonList("<name>");
                case 3: return Collections.singletonList("<fromX>");
                case 4: return Collections.singletonList("<fromZ>");
                case 5: return Collections.singletonList("<toX>");
                case 6: return Collections.singletonList("<toZ>");
                case 7: return Collections.singletonList("<fallY>");
                case 8: return Collections.singletonList("<tpX>");
                case 9: return Collections.singletonList("<tpY>");
                case 10: return Collections.singletonList("<tpZ>");
                case 11: return Collections.singletonList("<message>");
            }
        }

        if (args[0].equalsIgnoreCase("rename") && args.length == 3) {
            return Collections.singletonList("<newName>");
        }

        if (args[0].equalsIgnoreCase("edit")) {
            if (args.length == 3) {
                return Arrays.asList("fromx", "fromz", "tox", "toz", "fally", "tpx", "tpy", "tpz", "message", "enabled");
            }
            if (args.length > 3 && args.length % 2 == 1) {
                return Arrays.asList("<value>");
            }
        }

        return Collections.emptyList();
    }

    // ---------------- Region Class ----------------
    private static class VoidRegion {
        String name;
        final String worldName;
        int fromX, fromZ, toX, toZ;
        int fallY;
        int tpX, tpY, tpZ;
        String message;
        boolean enabled;

        VoidRegion(String name, String worldName, int fromX, int fromZ, int toX, int toZ,
                   int fallY, int tpX, int tpY, int tpZ, String message, boolean enabled) {
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
            this.message = message;
            this.enabled = enabled;
        }

        int getMinX() { return Math.min(fromX, toX); }
        int getMaxX() { return Math.max(fromX, toX); }
        int getMinZ() { return Math.min(fromZ, toZ); }
        int getMaxZ() { return Math.max(fromZ, toZ); }
    }
}

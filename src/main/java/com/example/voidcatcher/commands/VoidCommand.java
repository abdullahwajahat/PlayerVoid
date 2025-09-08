package com.example.voidcatcher.commands;

import com.example.voidcatcher.VoidManagerPlugin;
import com.example.voidcatcher.model.VoidRegion;
import com.example.voidcatcher.model.VoidRegion.MessageMode;
import com.example.voidcatcher.util.Messages;
import com.example.voidcatcher.util.RegionVisualizer;
import com.example.voidcatcher.util.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class VoidCommand implements CommandExecutor, TabCompleter {

    private final VoidManagerPlugin plugin;
    private final Messages msg;

    public VoidCommand(VoidManagerPlugin plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] a) {
        if (a.length == 0) {
            sender.sendMessage(msg.comp(msg.prefixed("usage.base", "&eUse /void for help")));
            return true;
        }

        String sub = a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "pos1" -> doPos(sender, true);
            case "pos2" -> doPos(sender, false);
            case "create" -> doCreate(sender, a);
            case "remove" -> doRemove(sender, a);
            case "list" -> doList(sender);
            case "info" -> doInfo(sender, a);
            case "toggle" -> doToggle(sender, a);
            case "edit" -> doEdit(sender, a);
            case "bypass" -> doBypass(sender);
            case "reload" -> doReload(sender);
            default -> sender.sendMessage(msg.comp(msg.prefixed("unknown-subcommand", "&cUnknown subcommand.")));
        }
        return true;
    }

    // --- pos1/pos2 ---
    private void doPos(CommandSender sender, boolean first) {
        if (!(sender.hasPermission("voidmanager.edit") || sender.hasPermission("voidmanager.admin"))) {
            sender.sendMessage(msg.comp(msg.prefixed("no-permission", "&cNo permission")));
            return;
        }
        Player p = Util.asPlayer(sender);
        if (p == null) { sender.sendMessage(msg.comp(msg.prefixed("player-only", "&cPlayers only"))); return; }

        Location l = p.getLocation();
        if (first) {
            plugin.setPos1(p.getUniqueId(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
            sender.sendMessage(msg.comp(msg.prefixed("pos1-set", "&aPos1 set").replace("%x%", ""+l.getBlockX()).replace("%y%", ""+l.getBlockY()).replace("%z%", ""+l.getBlockZ())));
        } else {
            plugin.setPos2(p.getUniqueId(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
            sender.sendMessage(msg.comp(msg.prefixed("pos2-set", "&aPos2 set").replace("%x%", ""+l.getBlockX()).replace("%y%", ""+l.getBlockY()).replace("%z%", ""+l.getBlockZ())));
        }
    }

    // --- create ---
    private void doCreate(CommandSender sender, String[] a) {
        if (!(sender.hasPermission("voidmanager.create") || sender.hasPermission("voidmanager.admin"))) {
            sender.sendMessage(msg.comp(msg.prefixed("no-permission", "&cNo permission.")));
            return;
        }
        if (a.length < 2) {
            sender.sendMessage(msg.comp(msg.prefixed("usage.create-pos", "")));
            sender.sendMessage(msg.comp(msg.prefixed("usage.create-full", "")));
            return;
        }
        Player p = Util.asPlayer(sender);
        String name = a[1];
        if (plugin.getRegions().containsKey(name)) {
            sender.sendMessage(msg.comp(msg.prefixed("region.exists", "&cRegion exists").replace("%region%", name)));
            return;
        }

        // Try WE-style: /void create <name> <fallY> <tpX> <tpY> <tpZ> [yaw] [pitch] [message]
        if (p != null && a.length >= 4 && Util.isInt(a[2]) && Util.isInt(a[3])) {
            int[] pos1 = plugin.getPos1(p.getUniqueId());
            int[] pos2 = plugin.getPos2(p.getUniqueId());
            if (pos1 == null || pos2 == null) {
                sender.sendMessage(msg.comp(msg.prefixed("pos-missing", "&cSet pos1 and pos2 first.")));
                return;
            }
            int fallY = Integer.parseInt(a[2]);
            int tpX = Integer.parseInt(a[3]);
            if (a.length < 5) { sender.sendMessage(msg.comp(msg.prefixed("usage.create-pos", ""))); return; }
            int tpY = Integer.parseInt(a[4]);
            if (a.length < 6) { sender.sendMessage(msg.comp(msg.prefixed("usage.create-pos", ""))); return; }
            int tpZ = Integer.parseInt(a[5]);

            float yaw = 0f, pitch = 0f;
            int idx = 6;
            if (a.length > idx && Util.isFloat(a[idx])) { yaw = Float.parseFloat(a[idx++]); }
            if (a.length > idx && Util.isFloat(a[idx])) { pitch = Float.parseFloat(a[idx++]); }

            String messageText;
            if (a.length > idx) {
                messageText = Util.joinUnderscoreFrom(a, idx);
            } else {
                messageText = msg.raw("teleport.default", "&cYou fell into the void and were teleported!");
            }

            String world = p.getWorld().getName();
            VoidRegion r = new VoidRegion(
                    name, world,
                    pos1[0], pos1[2], pos2[0], pos2[2],
                    fallY, tpX, tpY, tpZ,
                    yaw, pitch,
                    true, 0,
                    messageText, MessageMode.CHAT,
                    "ENTITY_ENDERMAN_TELEPORT"
            );
            plugin.getRegions().put(name, r);
            plugin.saveRegion(r);

            sender.sendMessage(msg.comp(msg.prefixed("region.created", "&aRegion created").replace("%region%", name).replace("%world%", world)));
            if (plugin.isShowParticles() && p != null) RegionVisualizer.show(plugin, p, r, plugin.getParticleSeconds());
            return;
        }

        // Manual full form:
        // /void create <name> <fromX> <fromZ> <toX> <toZ> <fallY> <tpX> <tpY> <tpZ> [yaw] [pitch] [message]
        if (a.length < 10) {
            sender.sendMessage(msg.comp(msg.prefixed("usage.create-full", "")));
            return;
        }
        try {
            int fromX = Integer.parseInt(a[2]);
            int fromZ = Integer.parseInt(a[3]);
            int toX = Integer.parseInt(a[4]);
            int toZ = Integer.parseInt(a[5]);
            int fallY = Integer.parseInt(a[6]);
            int tpX = Integer.parseInt(a[7]);
            int tpY = Integer.parseInt(a[8]);
            int tpZ = Integer.parseInt(a[9]);

            float yaw = 0f, pitch = 0f;
            int idx = 10;
            if (a.length > idx && Util.isFloat(a[idx])) { yaw = Float.parseFloat(a[idx++]); }
            if (a.length > idx && Util.isFloat(a[idx])) { pitch = Float.parseFloat(a[idx++]); }

            String messageText;
            if (a.length > idx) {
                messageText = Util.joinUnderscoreFrom(a, idx);
            } else {
                messageText = msg.raw("teleport.default", "&cYou fell into the void and were teleported!");
            }

            String world = (sender instanceof Player pl) ? pl.getWorld().getName() : Bukkit.getWorlds().get(0).getName();
            VoidRegion r = new VoidRegion(
                    name, world,
                    fromX, fromZ, toX, toZ,
                    fallY, tpX, tpY, tpZ,
                    yaw, pitch,
                    true, 0,
                    messageText, MessageMode.CHAT,
                    "ENTITY_ENDERMAN_TELEPORT"
            );
            plugin.getRegions().put(name, r);
            plugin.saveRegion(r);
            sender.sendMessage(msg.comp(msg.prefixed("region.created", "&aRegion created").replace("%region%", name).replace("%world%", world)));
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text("§cAll numeric args must be numbers."));
        }
    }

    // --- remove ---
    private void doRemove(CommandSender sender, String[] a) {
        if (!(sender.hasPermission("voidmanager.remove") || sender.hasPermission("voidmanager.admin"))) {
            sender.sendMessage(msg.comp(msg.prefixed("no-permission", "&cNo permission.")));
            return;
        }
        if (a.length < 2) { sender.sendMessage(msg.comp(msg.prefixed("usage.remove", ""))); return; }
        String name = a[1];
        if (!plugin.getRegions().containsKey(name)) {
            sender.sendMessage(msg.comp(msg.prefixed("region.not-found", "").replace("%region%", name)));
            return;
        }
        plugin.deleteRegion(name);
        sender.sendMessage(msg.comp(msg.prefixed("region.removed", "").replace("%region%", name)));
    }

    // --- list ---
    private void doList(CommandSender sender) {
        if (!(sender.hasPermission("voidmanager.list") || sender.hasPermission("voidmanager.admin"))) {
            sender.sendMessage(msg.comp(msg.prefixed("no-permission", "&cNo permission.")));
            return;
        }
        sender.sendMessage(msg.comp(msg.raw("region.list-header", "&6Regions:")));
        for (VoidRegion r : plugin.getRegions().values().stream()
                .sorted(Comparator.comparing((VoidRegion x) -> x.worldName).thenComparing(x -> x.name))
                .toList()) {
            String line = msg.raw("region.list-item", "&7- %region% (world=%world%, enabled=%enabled%, priority=%priority%)")
                    .replace("%region%", r.name)
                    .replace("%world%", r.worldName)
                    .replace("%enabled%", String.valueOf(r.enabled))
                    .replace("%priority%", String.valueOf(r.priority));
            sender.sendMessage(msg.comp(line));
        }
    }

    // --- info ---
    private void doInfo(CommandSender sender, String[] a) {
        if (!(sender.hasPermission("voidmanager.list") || sender.hasPermission("voidmanager.admin"))) {
            sender.sendMessage(msg.comp(msg.prefixed("no-permission", "&cNo permission.")));
            return;
        }
        if (a.length < 2) { sender.sendMessage(msg.comp(msg.prefixed("usage.info", ""))); return; }
        String name = a[1];
        VoidRegion r = plugin.getRegions().get(name);
        if (r == null) { sender.sendMessage(msg.comp(msg.prefixed("region.not-found", "").replace("%region%", name))); return; }

        sender.sendMessage(msg.comp(msg.raw("region.info-header", "&6Region: %region%").replace("%region%", r.name)));
        sender.sendMessage(msg.comp(msg.raw("region.info-line1", "").replace("%world%", r.worldName).replace("%enabled%", ""+r.enabled).replace("%priority%", ""+r.priority)));
        sender.sendMessage(msg.comp(msg.raw("region.info-line2", "")
                .replace("%minx%", ""+r.getMinX()).replace("%maxx%", ""+r.getMaxX())
                .replace("%minz%", ""+r.getMinZ()).replace("%maxz%", ""+r.getMaxZ())
                .replace("%fally%", ""+r.fallY)));
        sender.sendMessage(msg.comp(msg.raw("region.info-line3", "")
                .replace("%x%", ""+r.tpX).replace("%y%", ""+r.tpY).replace("%z%", ""+r.tpZ)
                .replace("%yaw%", ""+r.tpYaw).replace("%pitch%", ""+r.tpPitch)));
        sender.sendMessage(msg.comp(msg.raw("region.info-line4", "")
                .replace("%mode%", r.messageMode.name().toLowerCase(Locale.ROOT))
                .replace("%message%", r.messageText == null ? "" : r.messageText)
                .replace("%sound%", r.sound == null ? "none" : r.sound)));
        if (sender instanceof Player p && plugin.isShowParticles()) {
            RegionVisualizer.show(plugin, p, r, plugin.getParticleSeconds());
        }
    }

    // --- toggle ---
    private void doToggle(CommandSender sender, String[] a) {
        if (!(sender.hasPermission("voidmanager.toggle") || sender.hasPermission("voidmanager.admin"))) {
            sender.sendMessage(msg.comp(msg.prefixed("no-permission", "&cNo permission.")));
            return;
        }
        if (a.length < 2) { sender.sendMessage(msg.comp(msg.prefixed("usage.info", ""))); return; }
        String name = a[1];
        VoidRegion r = plugin.getRegions().get(name);
        if (r == null) { sender.sendMessage(msg.comp(msg.prefixed("region.not-found", "").replace("%region%", name))); return; }
        r.enabled = !r.enabled;
        plugin.saveRegion(r);
        sender.sendMessage(msg.comp(msg.prefixed("region.toggled", "").replace("%region%", name).replace("%enabled%", ""+r.enabled)));
    }

    // --- edit ---
    private void doEdit(CommandSender sender, String[] a) {
        if (!(sender.hasPermission("voidmanager.edit") || sender.hasPermission("voidmanager.admin"))) {
            sender.sendMessage(msg.comp(msg.prefixed("no-permission", "&cNo permission.")));
            return;
        }
        if (a.length < 3) { sender.sendMessage(msg.comp(msg.prefixed("usage.edit", ""))); return; }
        String name = a[1];
        VoidRegion r = plugin.getRegions().get(name);
        if (r == null) { sender.sendMessage(msg.comp(msg.prefixed("region.not-found", "").replace("%region%", name))); return; }

        String field = a[2].toLowerCase(Locale.ROOT);
        try {
            switch (field) {
                case "message" -> {
                    if (a.length < 5) {
                        sender.sendMessage(msg.comp(msg.color("§e/void edit " + name + " message <chat|title|actionbar> <message...>")));
                        return;
                    }
                    MessageMode mode = MessageMode.valueOf(a[3].toUpperCase(Locale.ROOT));
                    String text = Util.joinUnderscoreFrom(a, 4);
                    r.messageMode = mode;
                    r.messageText = text;
                    plugin.saveRegion(r);
                    sendUpdated(sender, name, "message", mode.name().toLowerCase(Locale.ROOT) + " '" + text + "'");
                }
                case "pos1" -> {
                    Player p = Util.asPlayer(sender);
                    if (a.length == 3) {
                        if (p == null) { sender.sendMessage(msg.comp(msg.prefixed("player-only", ""))); return; }
                        r.fromX = p.getLocation().getBlockX();
                        r.fromZ = p.getLocation().getBlockZ();
                    } else if (a.length >= 5) {
                        r.fromX = Integer.parseInt(a[3]);
                        r.fromZ = Integer.parseInt(a[4]);
                    } else { sender.sendMessage(msg.comp(msg.color("§e/void edit " + name + " pos1 [x] [z]"))); return; }
                    plugin.saveRegion(r);
                    sendUpdated(sender, name, "pos1", r.fromX + "," + r.fromZ);
                }
                case "pos2" -> {
                    Player p = Util.asPlayer(sender);
                    if (a.length == 3) {
                        if (p == null) { sender.sendMessage(msg.comp(msg.prefixed("player-only", ""))); return; }
                        r.toX = p.getLocation().getBlockX();
                        r.toZ = p.getLocation().getBlockZ();
                    } else if (a.length >= 5) {
                        r.toX = Integer.parseInt(a[3]);
                        r.toZ = Integer.parseInt(a[4]);
                    } else { sender.sendMessage(msg.comp(msg.color("§e/void edit " + name + " pos2 [x] [z]"))); return; }
                    plugin.saveRegion(r);
                    sendUpdated(sender, name, "pos2", r.toX + "," + r.toZ);
                }
                case "fally" -> {
                    if (a.length < 4) { sender.sendMessage(msg.comp(msg.color("§e/void edit " + name + " fallY <value>"))); return; }
                    r.fallY = Integer.parseInt(a[3]);
                    plugin.saveRegion(r);
                    sendUpdated(sender, name, "fallY", String.valueOf(r.fallY));
                }
                case "tpcoords" -> {
                    if (a.length < 6) { sender.sendMessage(msg.comp(msg.color("§e/void edit " + name + " tpcoords <x> <y> <z> [yaw] [pitch]"))); return; }
                    r.tpX = Integer.parseInt(a[3]);
                    r.tpY = Integer.parseInt(a[4]);
                    r.tpZ = Integer.parseInt(a[5]);
                    int idx = 6;
                    if (a.length > idx) r.tpYaw = Float.parseFloat(a[idx++]);
                    if (a.length > idx) r.tpPitch = Float.parseFloat(a[idx]);
                    plugin.saveRegion(r);
                    sendUpdated(sender, name, "tpcoords", r.tpX+","+r.tpY+","+r.tpZ+" "+r.tpYaw+","+r.tpPitch);
                }
                case "yaw" -> {
                    if (a.length < 4) { sender.sendMessage(msg.comp(msg.color("§e/void edit " + name + " yaw <value>"))); return; }
                    r.tpYaw = Float.parseFloat(a[3]);
                    plugin.saveRegion(r);
                    sendUpdated(sender, name, "yaw", String.valueOf(r.tpYaw));
                }
                case "pitch" -> {
                    if (a.length < 4) { sender.sendMessage(msg.comp(msg.color("§e/void edit " + name + " pitch <value>"))); return; }
                    r.tpPitch = Float.parseFloat(a[3]);
                    plugin.saveRegion(r);
                    sendUpdated(sender, name, "pitch", String.valueOf(r.tpPitch));
                }
                case "enabled" -> {
                    if (a.length < 4) { sender.sendMessage(msg.comp(msg.color("§e/void edit " + name + " enabled <true|false>"))); return; }
                    r.enabled = Boolean.parseBoolean(a[3]);
                    plugin.saveRegion(r);
                    sendUpdated(sender, name, "enabled", String.valueOf(r.enabled));
                }
                case "priority" -> {
                    if (a.length < 4) { sender.sendMessage(msg.comp(msg.color("§e/void edit " + name + " priority <int>"))); return; }
                    r.priority = Integer.parseInt(a[3]);
                    plugin.saveRegion(r);
                    sendUpdated(sender, name, "priority", String.valueOf(r.priority));
                }
                case "sound" -> {
                    if (a.length < 4) { sender.sendMessage(msg.comp(msg.color("§e/void edit " + name + " sound <SOUND_NAME|none>"))); return; }
                    r.sound = a[3].equalsIgnoreCase("none") ? null : a[3].toUpperCase(Locale.ROOT);
                    plugin.saveRegion(r);
                    sendUpdated(sender, name, "sound", String.valueOf(r.sound));
                }
                case "type" -> {
                    if (a.length < 4) { sender.sendMessage(msg.comp(msg.color("§e/void edit " + name + " type <chat|title|actionbar>"))); return; }
                    r.messageMode = MessageMode.valueOf(a[3].toUpperCase(Locale.ROOT));
                    plugin.saveRegion(r);
                    sendUpdated(sender, name, "type", r.messageMode.name().toLowerCase(Locale.ROOT));
                }
                default -> sender.sendMessage(msg.comp(msg.prefixed("usage.edit", "")));
            }
        } catch (Exception ex) {
            sender.sendMessage(msg.comp("§cInvalid args: " + ex.getMessage()));
        }
    }

    private void sendUpdated(CommandSender sender, String name, String field, String val) {
        sender.sendMessage(msg.comp(msg.prefixed("region.updated", "&aRegion updated")
                .replace("%region%", name)
                .replace("%field%", field)
                .replace("%value%", val)));
    }

    // --- bypass ---
    private void doBypass(CommandSender sender) {
        if (!(sender.hasPermission("voidmanager.bypass") || sender.hasPermission("voidmanager.admin"))) {
            sender.sendMessage(msg.comp(msg.prefixed("no-permission", "&cNo permission.")));
            return;
        }
        Player p = Util.asPlayer(sender);
        if (p == null) { sender.sendMessage(msg.comp(msg.prefixed("player-only", "&cPlayers only"))); return; }
        boolean newState = plugin.toggleBypass(p.getUniqueId(), p);
        if (newState) {
            p.sendMessage(msg.comp(msg.prefixed("bypass.enabled", "&aBypass enabled.")));
        } else {
            p.sendMessage(msg.comp(msg.prefixed("bypass.disabled", "&cBypass disabled.")));
        }
    }

    // --- reload ---
    private void doReload(CommandSender sender) {
        if (!sender.hasPermission("voidmanager.admin")) {
            sender.sendMessage(msg.comp(msg.prefixed("no-permission", "&cNo permission.")));
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(msg.comp(msg.prefixed("reloaded", "&aReloaded config and regions.")));
    }

    // --- tab complete ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] a) {
        if (a.length == 1) {
            return Util.suggest(a[0], List.of("pos1","pos2","create","remove","list","info","toggle","edit","bypass","reload"));
        }
        if (a.length >= 2) {
            String sub = a[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "remove", "info", "toggle" -> {
                    return Util.suggest(a[1], plugin.getRegions().keySet());
                }
                case "edit" -> {
                    if (a.length == 2) {
                        return Util.suggest(a[1], plugin.getRegions().keySet());
                    }
                    if (a.length == 3) {
                        return Util.suggest(a[2], List.of("message","pos1","pos2","fallY","tpcoords","yaw","pitch","enabled","priority","sound","type"));
                    }
                    if (a.length == 4 && a[2].equalsIgnoreCase("sound")) {
                        return Util.suggest(a[3], Util.sounds());
                    }
                }
            }
        }
        return Collections.emptyList();
    }
}

package com.example.voidcatcher.util;

import com.example.voidcatcher.model.VoidRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Messages {
    private final JavaPlugin plugin;
    private FileConfiguration cfg;
    private String prefix;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        this.cfg = Yaml.load(file);
        this.prefix = color(cfg.getString("prefix", "&6[VoidManager]&r "));
    }

    public String raw(String path, String def) {
        return cfg.getString(path, def);
    }

    public String prefixed(String path, String def) {
        return prefix + color(cfg.getString(path, def));
    }

    public String color(String s) {
        return s == null ? "" : s.replace("&", "§");
    }

    public Component comp(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(color(s));
    }

    public void send(Player p, String path, String def) {
        p.sendMessage(comp(prefixed(path, def)));
    }

    public void sendMode(Player p, VoidRegion r, String defaultText) {
        String text = r.messageText == null || r.messageText.isBlank()
                ? cfg.getString("teleport.default", defaultText)
                : r.messageText;

        text = applyPlaceholders(text, p, r);

        switch (r.messageMode) {
            case CHAT -> p.sendMessage(comp(prefix + text));
            case TITLE -> p.showTitle(net.kyori.adventure.title.Title.title(
                    comp(text),
                    Component.empty()
            ));
            case ACTIONBAR -> p.sendActionBar(comp(text));
        }
    }

    public String applyPlaceholders(String text, Player p, VoidRegion r) {
        return color(text)
                .replace("%player%", p.getName())
                .replace("%region%", r.name)
                .replace("%world%", r.worldName)
                .replace("%x%", String.valueOf(r.tpX))
                .replace("%y%", String.valueOf(r.tpY))
                .replace("%z%", String.valueOf(r.tpZ));
    }
}

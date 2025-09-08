package com.example.voidcatcher.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Yaml {
    public static FileConfiguration load(File file) {
        return YamlConfiguration.loadConfiguration(file);
    }

    public static void save(FileConfiguration cfg, File file) {
        try {
            cfg.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

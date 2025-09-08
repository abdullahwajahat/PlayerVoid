package com.example.voidcatcher.util;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class Util {
    public static boolean isInt(String s) { try { Integer.parseInt(s); return true; } catch (Exception e) { return false; } }
    public static boolean isFloat(String s) { try { Float.parseFloat(s); return true; } catch (Exception e) { return false; } }

    public static Player asPlayer(CommandSender sender) {
        return (sender instanceof Player) ? (Player) sender : null;
    }

    public static List<String> sounds() {
        return Arrays.stream(Sound.values()).map(Enum::name).collect(Collectors.toList());
    }

    public static String joinFrom(String[] arr, int start) {
        if (start >= arr.length) return "";
        return String.join(" ", Arrays.copyOfRange(arr, start, arr.length));
    }

    public static String joinUnderscoreFrom(String[] arr, int start) {
        if (start >= arr.length) return "";
        return String.join(" ", Arrays.stream(Arrays.copyOfRange(arr, start, arr.length))
                .map(s -> s.replace("_", " ")).toList());
    }

    public static String color(String s) {
        return s == null ? "" : s.replace("&", "§");
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static <T> List<T> suggest(String prefix, Collection<T> options) {
        if (prefix == null || prefix.isEmpty()) return new ArrayList<>(options);
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toString().toLowerCase(Locale.ROOT).startsWith(p)).map(o -> o).collect(Collectors.toList());
    }
}

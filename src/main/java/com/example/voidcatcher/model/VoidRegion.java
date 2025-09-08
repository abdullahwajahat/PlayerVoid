package com.example.voidcatcher.model;

import org.bukkit.World;

public class VoidRegion {
    public String name;
    public String worldName;
    public int fromX, fromZ, toX, toZ;
    public int fallY;
    public int tpX, tpY, tpZ;
    public float tpYaw, tpPitch;
    public boolean enabled;
    public int priority;
    public String messageText;
    public MessageMode messageMode;
    public String sound; // Bukkit Sound name or null

    public enum MessageMode { CHAT, TITLE, ACTIONBAR }

    public VoidRegion(String name, String worldName,
                      int fromX, int fromZ, int toX, int toZ,
                      int fallY, int tpX, int tpY, int tpZ,
                      float tpYaw, float tpPitch,
                      boolean enabled, int priority,
                      String messageText, MessageMode messageMode,
                      String sound) {
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
        this.tpYaw = tpYaw;
        this.tpPitch = tpPitch;
        this.enabled = enabled;
        this.priority = priority;
        this.messageText = messageText;
        this.messageMode = messageMode;
        this.sound = sound;
    }

    public int getMinX() { return Math.min(fromX, toX); }
    public int getMaxX() { return Math.max(fromX, toX); }
    public int getMinZ() { return Math.min(fromZ, toZ); }
    public int getMaxZ() { return Math.max(fromZ, toZ); }

    public boolean containsXZ(int x, int z) {
        return x >= getMinX() && x <= getMaxX() && z >= getMinZ() && z <= getMaxZ();
    }
}

package com.example.voidcatcher;

public class VoidRegion {
    final String name;
    final String worldName;
    final int fromX, fromZ, toX, toZ;
    final int fallY;
    final int tpX, tpY, tpZ;
    boolean enabled;
    String message;

    public VoidRegion(String name, String worldName,
                      int fromX, int fromZ, int toX, int toZ,
                      int fallY, int tpX, int tpY, int tpZ,
                      boolean enabled, String message) {
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
        this.message = message;
    }

    int getMinX() { return Math.min(fromX, toX); }
    int getMaxX() { return Math.max(fromX, toX); }
    int getMinZ() { return Math.min(fromZ, toZ); }
    int getMaxZ() { return Math.max(fromZ, toZ); }
}

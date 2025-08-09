/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.waypoints;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.waypoints.events.WaypointAddedEvent;
import meteordevelopment.meteorclient.systems.waypoints.events.WaypointRemovedEvent;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.files.StreamUtils;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Waypoints extends System<Waypoints> implements Iterable<Waypoint> {
    public static final String[] BUILTIN_ICONS = {"square", "circle", "triangle", "star", "diamond", "skull"};

    public final Map<String, AbstractTexture> icons = new ConcurrentHashMap<>();

    private final List<Waypoint> waypoints = Collections.synchronizedList(new ArrayList<>());

    private final List<WaypointSet> waypointSets = new ArrayList<>();
    private WaypointSet defaultWaypointSet = null;

    public Waypoints() {
        super(null);
    }

    public static Waypoints get() {
        return Systems.get(Waypoints.class);
    }

    @Override
    public void init() {
        File iconsFolder = new File(new File(MeteorClient.FOLDER, "waypoints"), "icons");
        iconsFolder.mkdirs();

        for (String builtinIcon : BUILTIN_ICONS) {
            File iconFile = new File(iconsFolder, builtinIcon + ".png");
            if (!iconFile.exists()) copyIcon(iconFile);
        }

        File[] files = iconsFolder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.getName().endsWith(".png")) {
                try {
                    String name = file.getName().replace(".png", "");
                    AbstractTexture texture = new NativeImageBackedTexture(null, NativeImage.read(new FileInputStream(file)));
                    icons.put(name, texture);
                }
                catch (IOException e) {
                    MeteorClient.LOG.error("Failed to read a waypoint icon", e);
                }
            }
        }
    }

    @NotNull
    public WaypointSet getDefaultWaypointSet() {
        return defaultWaypointSet;
    }

    public void setDefaultWaypointSet(WaypointSet set) {
        addWaypointSet(set);
        defaultWaypointSet = set;
    }

    public WaypointSet getWaypointSet(String name) {
        for (WaypointSet set : waypointSets) {
            if (set.name.get().equalsIgnoreCase(name)) return set;
        }
        return null;
    }

    public WaypointSet getWaypointSet(UUID uuid) {
       for (WaypointSet set : waypointSets) {
            if (set.uuid.equals(uuid)) return set;
       }
       return null;
    }

    public void addWaypointSet(WaypointSet set) {
        if (!waypointSets.contains(set)) waypointSets.add(set);
        save();
    }

    public boolean removeWaypointSet(WaypointSet set, boolean deleteWaypoints) {
        if (set == defaultWaypointSet) return false;

        if (!waypointSets.contains(set)) return false;
        for (Waypoint waypoint : set) {
            if (deleteWaypoints) remove(waypoint);
            else waypoint.setWaypointSet(null);
        }

        waypointSets.remove(set);

        save();
        return true;
    }

    public Iterable<WaypointSet> waypointSets() {
        return waypointSets;
    }

    /**
     * Adds a waypoint or saves it if it already exists
     * @return {@code true} if waypoint already exists
     */
    public boolean add(Waypoint waypoint) {
        if (waypoints.contains(waypoint)) {
            save();
            return true;
        }

        waypoints.add(waypoint);
        waypoint.setWaypointSet(waypoint.getWaypointSet());
        save();

        MeteorClient.EVENT_BUS.post(new WaypointAddedEvent(waypoint));

        return false;
    }

    public boolean remove(Waypoint waypoint) {
        boolean removed = waypoints.remove(waypoint);
        if (removed) {
            waypoint.setWaypointSet(null);
            save();
            MeteorClient.EVENT_BUS.post(new WaypointRemovedEvent(waypoint));
        }

        return removed;
    }

    public Waypoint get(String name) {
        for (Waypoint waypoint : waypoints) {
            if (waypoint.name.get().equalsIgnoreCase(name)) return waypoint;
        }

        return null;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        load();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onGameDisconnected(GameLeftEvent event) {
        waypoints.clear();
    }

    public static boolean checkDimension(Waypoint waypoint) {
        Dimension playerDim = PlayerUtils.getDimension();
        Dimension waypointDim = waypoint.dimension.get();

        if (playerDim == waypointDim) return true;
        if (!waypoint.opposite.get()) return false;

        boolean playerOpp = playerDim == Dimension.Overworld || playerDim == Dimension.Nether;
        boolean waypointOpp = waypointDim == Dimension.Overworld || waypointDim == Dimension.Nether;

        return playerOpp && waypointOpp;
    }

    @Override
    public File getFile() {
        if (!Utils.canUpdate()) return null;
        return new File(new File(MeteorClient.FOLDER, "waypoints"), Utils.getFileWorldName() + ".nbt");
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    @Override
    public @NotNull Iterator<Waypoint> iterator() {
        return new WaypointIterator();
    }

    private void copyIcon(File file) {
        String path = "/assets/" + MeteorClient.MOD_ID + "/textures/icons/waypoints/" + file.getName();
        InputStream in = Waypoints.class.getResourceAsStream(path);

        if (in == null) {
            MeteorClient.LOG.error("Failed to read a resource: {}", path);
            return;
        }

        StreamUtils.copy(in, file);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("waypoint-sets", NbtUtils.listToTag(waypointSets));
        tag.put("waypoints", NbtUtils.listToTag(waypoints));

        tag.put("default-waypoint-set", Uuids.INT_STREAM_CODEC, defaultWaypointSet.uuid);

        return tag;
    }

    @Override
    public Waypoints fromTag(NbtCompound tag) {
        waypoints.clear();

        defaultWaypointSet = null;

        UUID defaultWaypointSetUUID = tag.get("default-waypoint-set", Uuids.INT_STREAM_CODEC).orElse(null);

        for (NbtElement element : tag.getListOrEmpty("waypoint-sets")) {
            WaypointSet set = new WaypointSet(element);
            if (set.uuid.equals(defaultWaypointSetUUID)) defaultWaypointSet = set;
            waypointSets.add(set);
        }

        if (defaultWaypointSet == null) {
            defaultWaypointSet = new WaypointSet();
            waypointSets.add(defaultWaypointSet);
        }

        for (NbtElement waypointTag : tag.getListOrEmpty("waypoints")) {
            waypoints.add(new Waypoint(waypointTag));
        }

        return this;
    }

    private final class WaypointIterator implements Iterator<Waypoint> {
        private final Iterator<Waypoint> it = waypoints.iterator();

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Waypoint next() {
            return it.next();
        }

        @Override
        public void remove() {
            it.remove();
            save();
        }
    }
}

/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.waypoints;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class WaypointSet implements Iterable<Waypoint>, ISerializable<WaypointSet> {

    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .defaultValue("Default")
        .build()
    );

    public final Setting<Boolean> visible = sgGeneral.add(new BoolSetting.Builder()
        .name("visible")
        .defaultValue(true)
        .build()
    );

    public final DefaultSettings defaultSettings = new DefaultSettings();

    public final UUID uuid;


    private final List<Waypoint> waypoints = new ArrayList<>();

    public WaypointSet() {
        uuid = UUID.randomUUID();
    }

    public WaypointSet(NbtElement fromElement) {
        NbtCompound nbt = (NbtCompound) fromElement;
        if (nbt.contains("uuid")) uuid = nbt.get("uuid", Uuids.INT_STREAM_CODEC).get();
        else uuid = UUID.randomUUID();
        fromTag(nbt);
    }

    public boolean remove(Waypoint waypoint) {
        if (waypoint.getWaypointSet() != this) return false;
        waypoint.setWaypointSet(null);
        return true;
    }

    public boolean removeWaypointUnsafe(Waypoint waypoint) {
        return waypoints.remove(waypoint);
    }

    public void add(Waypoint waypoint) {
        if (waypoints.contains(waypoint)) return;
        if (waypoint.getWaypointSet() != this) waypoint.setWaypointSet(this);
        else waypoints.add(waypoint);
    }

    public boolean isDefault() {
        return this == Waypoints.get().getDefaultWaypointSet();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("uuid", Uuids.INT_STREAM_CODEC, uuid);
        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public WaypointSet fromTag(NbtCompound tag) {
        if (tag.contains("settings")) {
            settings.fromTag(tag.getCompoundOrEmpty("settings"));
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Waypoint waypoint = (Waypoint) o;
        return Objects.equals(uuid, waypoint.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    @Override
    public String toString() {
        return name.get();
    }

    @Override
    public @NotNull Iterator<Waypoint> iterator() {
        return new SetWaypointIterator();
    }

    public final class DefaultSettings {
        private final SettingGroup sg = settings.createGroup("Defaults");

        public Setting<String> icon = sg.add(new ProvidedStringSetting.Builder()
            .name("icon")
            .description("The icon of the waypoint.")
            .defaultValue("Square")
            .supplier(() -> Waypoints.BUILTIN_ICONS)
            .onChanged(this::validateIcon)
            .build()
        );

        public Setting<SettingColor> color = sg.add(new ColorSetting.Builder()
            .name("color")
            .description("The color of the waypoint.")
            .defaultValue(MeteorClient.ADDON.color.toSetting())
            .build()
        );

        public Setting<Boolean> visible = sg.add(new BoolSetting.Builder()
            .name("visible")
            .description("Whether to show the waypoint.")
            .defaultValue(true)
            .build()
        );

        public Setting<Integer> maxVisible = sg.add(new IntSetting.Builder()
            .name("max-visible-distance")
            .description("How far away to render the waypoint.")
            .defaultValue(5000)
            .build()
        );

        public Setting<Double> scale = sg.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of the waypoint.")
            .defaultValue(1)
            .build()
        );

        public Setting<Boolean> opposite = sg.add(new BoolSetting.Builder()
            .name("opposite-dimension")
            .description("Whether to show the waypoint in the opposite dimension.")
            .defaultValue(true)
            .build()
        );

        private void validateIcon(String v) {
            Map<String, AbstractTexture> icons = Waypoints.get().icons;

            AbstractTexture texture = icons.get(v);
            if (texture == null && !icons.isEmpty()) {
                icon.set(icons.keySet().iterator().next());
            }
        }
    }

    private final class SetWaypointIterator implements Iterator<Waypoint> {
        private final Iterator<Waypoint> it = waypoints.iterator();

        private Waypoint current;

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Waypoint next() {
            return current = it.next();
        }

        @Override
        public void remove() {
            current.setWaypointSet(null);
            it.remove();
        }
    }
}

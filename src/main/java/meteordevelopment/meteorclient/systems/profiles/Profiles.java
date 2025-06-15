/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.profiles;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Profiles extends System<Profiles> implements Iterable<Profile> {
    public static final File FOLDER = new File(MeteorClient.FOLDER, "profiles");

    private List<Profile> profiles = new ArrayList<>();
    private Profile active = null;

    public Profiles() {
        super("profiles");
    }

    public static Profiles get() {
        return Systems.get(Profiles.class);
    }


    public void add(Profile profile) {
        if (!profiles.contains(profile)) profiles.add(profile);
        profile.save();
        save();
    }

    public void remove(Profile profile) {
        if (profiles.remove(profile)) {
            if (profile == active) active = null;
            profile.delete();
        }
        save();
    }

    public Profile get(String name) {
        for (Profile profile : this) {
            if (profile.name.get().equalsIgnoreCase(name)) {
                return profile;
            }
        }

        return null;
    }

    public Profile getActive() {
        return active;
    }

    public void setActive(String name) {
        setActive(get(name));
    }

    public void setActive(Profile profile) {
        if (active != null) active.autoSave();
        active = profile;
    }

    public List<Profile> getAll() {
        return profiles;
    }

    @Override
    public File getFile() {
        return new File(FOLDER, "profiles.nbt");
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        for (Profile profile : this) {
            if (profile.loadOnJoin.get().contains(Utils.getWorldName())) {
                profile.load();
            }
        }
    }

    public boolean isEmpty() {
        return profiles.isEmpty();
    }

    @Override
    public @NotNull Iterator<Profile> iterator() {
        return profiles.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("profiles", NbtUtils.listToTag(profiles));
        if (active != null) tag.put("active_profile", NbtString.of(active.name.get()));
        return tag;
    }

    @Override
    public Profiles fromTag(NbtCompound tag) {
        profiles = NbtUtils.listFromTag(tag.getListOrEmpty("profiles"), Profile::new);
        tag.getString("active_profile").ifPresent(this::setActive);
        return this;
    }

    @Override
    public void save(File folder) {
        super.save(folder);
        if (active != null) active.autoSave();
    }
}

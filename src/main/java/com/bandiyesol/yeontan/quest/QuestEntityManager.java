package com.bandiyesol.yeontan.quest;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class QuestEntityManager {

    private static final Set<UUID> AUTHORIZED_ENTITIES =  new HashSet<>();
    private static final File SAVE_FILE = new File("config/backto2020_entities.json");
    private static final Gson GSON = new Gson();

    public static void addEntity(UUID uuid){ AUTHORIZED_ENTITIES.add(uuid); }

    public static void removeEntity(UUID uuid){ AUTHORIZED_ENTITIES.remove(uuid); }

    public static boolean isAuthorized(UUID uuid){ return AUTHORIZED_ENTITIES.contains(uuid); }

    public static void saveData() {
        try (Writer writer = new FileWriter(SAVE_FILE)) {
            GSON.toJson(AUTHORIZED_ENTITIES, writer);
        }

        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void loadData() {
        try (Reader reader = new FileReader(SAVE_FILE)) {
            Set<UUID> loaded = GSON.fromJson(reader, new TypeToken<HashSet<UUID>>(){}.getType());

            if (loaded != null) {
                AUTHORIZED_ENTITIES.clear();
                AUTHORIZED_ENTITIES.addAll(loaded);
            }
        }

        catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}

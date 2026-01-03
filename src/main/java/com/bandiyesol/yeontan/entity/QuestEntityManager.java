package com.bandiyesol.yeontan.entity;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.entity.EntityCustomNpc;

import java.io.*;
import java.util.*;

public class QuestEntityManager {

    private static final Set<UUID> TEMPLATE_UUIDS =  new HashSet<>();
    private static final Map<UUID, NBTTagCompound> templateData = new HashMap<>();
    private static final File SAVE_DIR = new File("config/backto2020_templates");


    public static void addTemplate(UUID uuid, EntityCustomNpc npc) {
        NBTTagCompound nbt =  new NBTTagCompound();
        npc.writeToNBT(nbt);
        templateData.put(uuid, nbt);
        TEMPLATE_UUIDS.add(uuid);

        saveSingleTemplate(uuid, nbt);
    }

    public static void removeTemplate(UUID id) {
        TEMPLATE_UUIDS.remove(id);
        templateData.remove(id);
        File file = new File(SAVE_DIR, id.toString() + ".dat");
        if (file.exists()) {
            if (file.delete()) { System.out.println("성공: 템플릿 파일 삭제 완료 (" + id + ")"); }
            else { System.err.println("실패: 템플릿 파일을 삭제할 수 없습니다."); }
        }
    }

    public static boolean isTemplate(UUID id) {
        return TEMPLATE_UUIDS.contains(id);
    }

    public static NBTTagCompound getTemplateNbt(UUID uuid) { return templateData.get(uuid); }

    public static List<UUID> getTemplateList() { return new ArrayList<>(TEMPLATE_UUIDS); }

    private static void saveSingleTemplate(UUID uuid, NBTTagCompound nbt) {
        if (!SAVE_DIR.exists()) SAVE_DIR.mkdirs();
        File file = new File(SAVE_DIR, uuid.toString() + ".dat");
        try (OutputStream os = new FileOutputStream(file)) {
            CompressedStreamTools.writeCompressed(nbt, os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadData() {
        if (!SAVE_DIR.exists()) return;
        File[] files = SAVE_DIR.listFiles((dir, name) -> name.endsWith(".dat"));
        if (files == null) return;

        templateData.clear();
        TEMPLATE_UUIDS.clear();

        for (File file : files) {
            try (InputStream is = new FileInputStream(file)) {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
                UUID uuid = UUID.fromString(file.getName().replace(".dat", ""));

                templateData.put(uuid, nbt);
                TEMPLATE_UUIDS.add(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("성공: " + TEMPLATE_UUIDS.size() + "개의 퀘스트 템플릿 로드 완료.");
    }
}

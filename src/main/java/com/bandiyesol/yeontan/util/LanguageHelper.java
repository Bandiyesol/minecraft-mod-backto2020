package com.bandiyesol.yeontan.util;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LanguageHelper {
    
    private static final Map<String, String> koreanTranslations = new HashMap<>();
    private static boolean loaded = false;
    
    public static void loadKoreanTranslations() {
        if (loaded) return;
        
        try {
            // Backto2020 모드 찾기
            ModContainer iaddMod = Loader.instance().getModList().stream()
                    .filter(mod -> "iadd".equals(mod.getModId()))
                    .findFirst()
                    .orElse(null);
            
            if (iaddMod == null) {
                System.out.println("[Yeontan] iadd 모드를 찾을 수 없습니다.");
                return;
            }
            
            // 모드 파일 경로 가져오기
            String modSource = iaddMod.getSource().toString();
            if (modSource.startsWith("file:")) {
                modSource = modSource.substring(5);
            }
            
            // JAR 파일 열기
            try (ZipFile zipFile = new ZipFile(modSource)) {
                ZipEntry langEntry = zipFile.getEntry("assets/iadd/lang/ko_kr.lang");
                if (langEntry == null) {
                    System.out.println("[Yeontan] ko_kr.lang 파일을 찾을 수 없습니다.");
                    return;
                }
                
                // 언어 파일 읽기
                try (InputStream is = zipFile.getInputStream(langEntry);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        
                        int equalsIndex = line.indexOf('=');
                        if (equalsIndex > 0) {
                            String key = line.substring(0, equalsIndex).trim();
                            String value = line.substring(equalsIndex + 1).trim();
                            koreanTranslations.put(key, value);
                        }
                    }
                    
                    System.out.println("[Yeontan] 한글 번역 " + koreanTranslations.size() + "개를 로드했습니다.");
                    loaded = true;
                }
            }
        } catch (Exception e) {
            System.out.println("[Yeontan] 한글 번역 로드 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static String translateToKorean(String translationKey) {
        if (!loaded) {
            loadKoreanTranslations();
        }
        return koreanTranslations.getOrDefault(translationKey, translationKey);
    }
    
    public static String getItemKoreanName(String itemRegistryName) {
        // registry name에서 번역 키 생성: iadd:f_vegetable_bun -> item.iadd.f_vegetable_bun.name
        String[] parts = itemRegistryName.split(":");
        if (parts.length != 2) {
            return itemRegistryName;
        }
        
        String translationKey = "item." + parts[0] + "." + parts[1] + ".name";
        String koreanName = translateToKorean(translationKey);
        
        // 번역이 없으면 (번역 키와 결과가 같으면) null 반환
        if (koreanName.equals(translationKey)) {
            return null;
        }
        
        return koreanName;
    }
}

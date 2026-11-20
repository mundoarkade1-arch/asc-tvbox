package com.ascplay.tvbox;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Preferências simples do app, escolhidas em Ajustes. Tudo tem um valor padrão que mantém o
 * comportamento de antes (nada muda para quem nunca abriu esses ajustes).
 *
 * - Atualização: procurar versão nova ao abrir o app (padrão: sim).
 * - Reprodução: áudio e legenda preferidos e qualidade máxima (usados pelo player).
 * - Grupos de canais ao vivo ocultos na lista de categorias (o grupo Adultos não entra aqui:
 *   quem cuida dele é o controle parental).
 */
final class AppPrefs {

    private static final String PREFS = "asc_settings";
    private static final String K_AUTO_UPDATE = "auto_update";
    private static final String K_AUDIO = "audio_choice";
    private static final String K_TEXT = "text_choice";
    private static final String K_QUALITY = "quality_choice";
    private static final String K_HIDDEN = "hidden_groups";

    private AppPrefs() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- Atualização ----------

    static boolean autoUpdateCheck(Context c) {
        try {
            return prefs(c).getBoolean(K_AUTO_UPDATE, true);
        } catch (Exception e) {
            return true;
        }
    }

    static void setAutoUpdateCheck(Context c, boolean on) {
        prefs(c).edit().putBoolean(K_AUTO_UPDATE, on).apply();
    }

    // ---------- Reprodução ----------

    private static int choice(Context c, String key, int max) {
        try {
            int v = prefs(c).getInt(key, 0);
            return v >= 0 && v < max ? v : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** 0 automático, 1 português, 2 inglês. */
    static int audioChoice(Context c) {
        return choice(c, K_AUDIO, 3);
    }

    static void setAudioChoice(Context c, int v) {
        prefs(c).edit().putInt(K_AUDIO, v).apply();
    }

    /** Código do idioma para o player; vazio = automático. */
    static String audioLang(Context c) {
        int v = audioChoice(c);
        return v == 1 ? "pt" : (v == 2 ? "en" : "");
    }

    /** 0 automático, 1 português. */
    static int textChoice(Context c) {
        return choice(c, K_TEXT, 2);
    }

    static void setTextChoice(Context c, int v) {
        prefs(c).edit().putInt(K_TEXT, v).apply();
    }

    static String textLang(Context c) {
        return textChoice(c) == 1 ? "pt" : "";
    }

    /** 0 automática (até 1080p, como sempre foi), 1 até 720p, 2 até 480p. */
    static int qualityChoice(Context c) {
        return choice(c, K_QUALITY, 3);
    }

    static void setQualityChoice(Context c, int v) {
        prefs(c).edit().putInt(K_QUALITY, v).apply();
    }

    static int maxVideoWidth(Context c) {
        int v = qualityChoice(c);
        return v == 1 ? 1280 : (v == 2 ? 854 : 1920);
    }

    static int maxVideoHeight(Context c) {
        int v = qualityChoice(c);
        return v == 1 ? 720 : (v == 2 ? 480 : 1080);
    }

    // ---------- Grupos de canais ocultos ----------

    private static Set<String> hiddenSet(Context c) {
        try {
            return new HashSet<>(prefs(c).getStringSet(K_HIDDEN, new HashSet<String>()));
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    static boolean isGroupHidden(Context c, CategoryGroups.Group g) {
        if (g == null || g == CategoryGroups.Group.ADULTOS) {
            return false;
        }
        return hiddenSet(c).contains(String.valueOf(g.code));
    }

    static void setGroupHidden(Context c, CategoryGroups.Group g, boolean hidden) {
        if (g == null || g == CategoryGroups.Group.ADULTOS) {
            return;
        }
        Set<String> s = hiddenSet(c);
        if (hidden) {
            s.add(String.valueOf(g.code));
        } else {
            s.remove(String.valueOf(g.code));
        }
        prefs(c).edit().putStringSet(K_HIDDEN, s).apply();
    }

    static int hiddenCount(Context c) {
        int n = 0;
        for (CategoryGroups.Group g : CategoryGroups.Group.values()) {
            if (isGroupHidden(c, g)) {
                n++;
            }
        }
        return n;
    }
}

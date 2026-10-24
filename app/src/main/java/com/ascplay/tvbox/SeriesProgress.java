package com.ascplay.tvbox;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lembra em qual episódio a pessoa parou em cada série.
 *
 * Guarda só a posição do episódio na fila da série (um número pequeno) e a hora.
 * O nome ou o endereço da série NUNCA é guardado em texto: a chave é um resumo embaralhado
 * (SHA-1), porque o endereço da lista pode conter usuário e senha.
 * Guarda no máximo 50 séries; as mais antigas são esquecidas.
 */
public final class SeriesProgress {

    private static final String FILE = "series_progress";
    private static final int MAX_ENTRIES = 50;
    private static final int MAX_INDEX = 500;

    private SeriesProgress() {
    }

    /** Anota que a pessoa está no episódio de posição "index" da fila desta série. */
    public static void save(Context ctx, String seriesKey, int index) {
        if (ctx == null || seriesKey == null || seriesKey.isEmpty()
                || index < 0 || index >= MAX_INDEX) {
            return;
        }
        String k = hash(seriesKey);
        if (k == null) {
            return;
        }
        SharedPreferences sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString(k, index + "|" + System.currentTimeMillis());

        // Esquece as séries mais antigas se passar do limite
        Map<String, ?> all = sp.getAll();
        int count = all.size() + (all.containsKey(k) ? 0 : 1);
        Set<String> removed = new HashSet<>();
        while (count > MAX_ENTRIES) {
            String oldestKey = null;
            long oldestTime = Long.MAX_VALUE;
            for (Map.Entry<String, ?> en : all.entrySet()) {
                String key = en.getKey();
                if (key.equals(k) || removed.contains(key)) {
                    continue;
                }
                long t = timeOf(en.getValue());
                if (t < oldestTime) {
                    oldestTime = t;
                    oldestKey = key;
                }
            }
            if (oldestKey == null) {
                break;
            }
            ed.remove(oldestKey);
            removed.add(oldestKey);
            count--;
        }
        ed.apply();
    }

    /** Devolve a posição anotada, ou -1 se não há nada anotado para esta série. */
    public static int load(Context ctx, String seriesKey) {
        if (ctx == null || seriesKey == null || seriesKey.isEmpty()) {
            return -1;
        }
        String k = hash(seriesKey);
        if (k == null) {
            return -1;
        }
        SharedPreferences sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        String v;
        try {
            v = sp.getString(k, null);
        } catch (ClassCastException e) {
            return -1;
        }
        if (v == null) {
            return -1;
        }
        int bar = v.indexOf('|');
        String num = bar < 0 ? v : v.substring(0, bar);
        try {
            int index = Integer.parseInt(num);
            return (index >= 0 && index < MAX_INDEX) ? index : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static long timeOf(Object value) {
        if (!(value instanceof String)) {
            return 0L;
        }
        String v = (String) value;
        int bar = v.indexOf('|');
        if (bar < 0) {
            return 0L;
        }
        try {
            return Long.parseLong(v.substring(bar + 1));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}

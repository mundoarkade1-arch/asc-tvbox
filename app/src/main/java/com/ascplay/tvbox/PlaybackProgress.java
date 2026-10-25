package com.ascplay.tvbox;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;
import java.util.Map;

/**
 * Lembra o MINUTO exato onde a pessoa parou em cada filme e em cada episódio.
 *
 * Para cada item guarda só dois números pequenos (posição e duração, em milissegundos) e a hora.
 * O nome ou o endereço do vídeo NUNCA é guardado em texto: a chave é um resumo embaralhado
 * (SHA-1), porque o endereço da lista pode conter usuário e senha.
 * Guarda no máximo 300 itens; os mais antigos são esquecidos.
 *
 * Canal ao vivo não usa esta classe.
 */
public final class PlaybackProgress {

    private static final String FILE = "playback_progress";
    private static final int MAX_ENTRIES = 300;
    // Menos que isso, não vale a pena lembrar (a pessoa só abriu e fechou)
    private static final long MIN_SAVE_MS = 5000L;
    // Passou disso do vídeo: conta como "visto"
    private static final double FINISHED_FRACTION = 0.95;

    private PlaybackProgress() {
    }

    /** O que foi anotado para um item. */
    public static final class Mark {
        public final long positionMs;
        public final long durationMs;

        Mark(long positionMs, long durationMs) {
            this.positionMs = positionMs;
            this.durationMs = durationMs;
        }

        /** De 0.0 a 1.0: quanto do vídeo já foi visto. 0 se a duração não é conhecida. */
        public double fraction() {
            if (durationMs <= 0) {
                return 0.0;
            }
            double f = (double) positionMs / (double) durationMs;
            if (f < 0.0) {
                return 0.0;
            }
            return f > 1.0 ? 1.0 : f;
        }

        /** Já viu quase tudo (ou tudo). */
        public boolean isFinished() {
            return durationMs > 0 && fraction() >= FINISHED_FRACTION;
        }

        /** Viu uma parte, mas não terminou: vale o "Continuar de onde parei". */
        public boolean isPartial() {
            return positionMs >= MIN_SAVE_MS && !isFinished();
        }
    }

    // ---------- Chaves (o mesmo texto no player e nas páginas) ----------

    /** Filme do servidor. */
    public static String movieKey(String server, String user, String movieId) {
        return "xm|" + server + "|" + user + "|" + movieId;
    }

    /** Episódio do servidor (o número do episódio não muda). */
    public static String episodeKey(String server, String user, String episodeId) {
        return "xe|" + server + "|" + user + "|" + episodeId;
    }

    /** Filme ou episódio da lista M3U: vale o endereço do próprio vídeo. */
    public static String urlKey(String videoUrl) {
        return "mu|" + videoUrl;
    }

    // ---------- Guardar e ler ----------

    /**
     * Anota a posição atual. Ignora posições muito curtas e durações desconhecidas.
     * Pode ser chamada de tempos em tempos enquanto o vídeo toca.
     */
    public static void save(Context ctx, String key, long positionMs, long durationMs) {
        if (ctx == null || key == null || key.isEmpty()
                || positionMs < MIN_SAVE_MS || durationMs <= 0) {
            return;
        }
        if (positionMs > durationMs) {
            positionMs = durationMs;
        }
        String k = hash(key);
        if (k == null) {
            return;
        }
        SharedPreferences sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString(k, positionMs + "|" + durationMs + "|" + System.currentTimeMillis());

        // Só confere o limite quando o item é novo (para não gastar à toa)
        if (!sp.contains(k)) {
            Map<String, ?> all = sp.getAll();
            int count = all.size() + 1;
            while (count > MAX_ENTRIES) {
                String oldestKey = null;
                long oldestTime = Long.MAX_VALUE;
                for (Map.Entry<String, ?> en : all.entrySet()) {
                    String other = en.getKey();
                    if (other.equals(k)) {
                        continue;
                    }
                    long t = timeOf(en.getValue());
                    if (t < oldestTime) {
                        oldestTime = t;
                        oldestKey = other;
                    }
                }
                if (oldestKey == null) {
                    break;
                }
                ed.remove(oldestKey);
                all = copyWithout(all, oldestKey);
                count--;
            }
        }
        ed.apply();
    }

    /** Devolve o que foi anotado, ou null se nada foi anotado para este item. */
    public static Mark load(Context ctx, String key) {
        if (ctx == null || key == null || key.isEmpty()) {
            return null;
        }
        String k = hash(key);
        if (k == null) {
            return null;
        }
        SharedPreferences sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        String v;
        try {
            v = sp.getString(k, null);
        } catch (ClassCastException e) {
            return null;
        }
        if (v == null) {
            return null;
        }
        String[] parts = v.split("\\|");
        if (parts.length < 2) {
            return null;
        }
        try {
            long pos = Long.parseLong(parts[0]);
            long dur = Long.parseLong(parts[1]);
            if (pos < 0 || dur <= 0) {
                return null;
            }
            return new Mark(pos, dur);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------- Ajudas ----------

    private static Map<String, ?> copyWithout(Map<String, ?> all, String removedKey) {
        java.util.HashMap<String, Object> copy = new java.util.HashMap<>();
        for (Map.Entry<String, ?> en : all.entrySet()) {
            if (!en.getKey().equals(removedKey)) {
                copy.put(en.getKey(), en.getValue());
            }
        }
        return copy;
    }

    private static long timeOf(Object value) {
        if (!(value instanceof String)) {
            return 0L;
        }
        String[] parts = ((String) value).split("\\|");
        if (parts.length < 3) {
            return 0L;
        }
        try {
            return Long.parseLong(parts[2]);
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

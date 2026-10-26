package com.ascplay.tvbox;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Lista do "Continue assistindo": filmes e episódios começados e não terminados.
 * Guarda no máximo 25 itens, do mais novo para o mais antigo.
 * A lista inteira é criptografada (AES-GCM, chave do Android Keystore),
 * porque o endereço do vídeo pode ter usuário e senha.
 * Se o Keystore não funcionar, guarda sem criptografia
 * (dentro do armazenamento privado do app), como o CredentialStore.
 */
public final class ContinueStore {

    public static final int MAX_ITEMS = 25;

    private static final String PREFS = "asc_continue";
    private static final String KEY_LIST = "list";
    private static final String KEY_ALIAS = "asc_tvbox_key";
    private static final String PREFIX_ENC = "K:";
    private static final String PREFIX_PLAIN = "P:";

    /** Um item da lista. */
    public static final class Saved {
        public final String key;       // chave (SHA-1 do endereço do vídeo)
        public final String kind;      // "movie" ou "episode"
        public final String name;      // nome para mostrar no cartão
        public final String icon;      // endereço da capa (pode ser vazio)
        public final String url;       // endereço do vídeo
        public final String id;        // id do item
        public final String ext;       // extensão (mp4, mkv...)
        public final String seriesKey; // chave da série (vazio em filme)
        public final long posMs;       // minuto onde parou, em milissegundos
        public final long durMs;       // duração total, em milissegundos
        public final long time;        // hora em que foi salvo

        public Saved(String kind, String name, String icon, String url, String id,
                     String ext, String seriesKey, long posMs, long durMs) {
            this(keyFor(url), kind, name, icon, url, id, ext, seriesKey, posMs, durMs,
                    System.currentTimeMillis());
        }

        private Saved(String key, String kind, String name, String icon, String url,
                      String id, String ext, String seriesKey, long posMs, long durMs,
                      long time) {
            this.key = key;
            this.kind = kind == null ? "" : kind;
            this.name = name == null ? "" : name;
            this.icon = icon == null ? "" : icon;
            this.url = url == null ? "" : url;
            this.id = id == null ? "" : id;
            this.ext = ext == null ? "" : ext;
            this.seriesKey = seriesKey == null ? "" : seriesKey;
            this.posMs = posMs;
            this.durMs = durMs;
            this.time = time;
        }
    }

    private ContinueStore() {
    }

    /** Guarda o item no começo da lista. Se já existia, troca pelo novo. */
    public static synchronized void save(Context context, Saved item) {
        List<Saved> all = readAll(context);
        List<Saved> out = new ArrayList<>();
        out.add(item);
        for (Saved s : all) {
            if (!s.key.equals(item.key) && out.size() < MAX_ITEMS) {
                out.add(s);
            }
        }
        writeAll(context, out);
    }

    /** Devolve a lista, do mais novo para o mais antigo. */
    public static synchronized List<Saved> list(Context context) {
        return readAll(context);
    }

    /** Tira da lista o vídeo com este endereço (por exemplo, quando termina). */
    public static synchronized void remove(Context context, String url) {
        String key = keyFor(url);
        List<Saved> all = readAll(context);
        List<Saved> out = new ArrayList<>();
        for (Saved s : all) {
            if (!s.key.equals(key)) {
                out.add(s);
            }
        }
        if (out.size() != all.size()) {
            writeAll(context, out);
        }
    }

    public static synchronized void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    // ---------- Leitura e escrita ----------

    private static List<Saved> readAll(Context context) {
        List<Saved> out = new ArrayList<>();
        String stored = prefs(context).getString(KEY_LIST, null);
        if (stored == null || stored.isEmpty()) {
            return out;
        }
        try {
            JSONArray arr = new JSONArray(unprotect(stored));
            for (int i = 0; i < arr.length() && out.size() < MAX_ITEMS; i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Saved(
                        o.optString("key", ""),
                        o.optString("kind", ""),
                        o.optString("name", ""),
                        o.optString("icon", ""),
                        o.optString("url", ""),
                        o.optString("id", ""),
                        o.optString("ext", ""),
                        o.optString("seriesKey", ""),
                        o.optLong("posMs", 0L),
                        o.optLong("durMs", 0L),
                        o.optLong("time", 0L)));
            }
        } catch (Exception e) {
            // Chave perdida ou dado corrompido: apaga a lista e começa vazia.
            prefs(context).edit().clear().apply();
            out.clear();
        }
        return out;
    }

    private static void writeAll(Context context, List<Saved> list) {
        try {
            JSONArray arr = new JSONArray();
            for (Saved s : list) {
                JSONObject o = new JSONObject();
                o.put("key", s.key);
                o.put("kind", s.kind);
                o.put("name", s.name);
                o.put("icon", s.icon);
                o.put("url", s.url);
                o.put("id", s.id);
                o.put("ext", s.ext);
                o.put("seriesKey", s.seriesKey);
                o.put("posMs", s.posMs);
                o.put("durMs", s.durMs);
                o.put("time", s.time);
                arr.put(o);
            }
            prefs(context).edit().putString(KEY_LIST, protect(arr.toString())).apply();
        } catch (Exception e) {
            // Não conseguiu guardar: a lista fica como estava.
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Chave embaralhada (SHA-1) do endereço do vídeo. Nunca guarda o endereço como chave. */
    static String keyFor(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest((url == null ? "" : url).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(url == null ? 0 : url.hashCode());
        }
    }

    // ---------- Proteção dos valores ----------

    private static String protect(String plain) {
        byte[] data = plain.getBytes(StandardCharsets.UTF_8);
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                return PREFIX_ENC + encrypt(data);
            } catch (Exception e) {
                // Keystore com problema neste aparelho: usa o plano B.
            }
        }
        return PREFIX_PLAIN + Base64.encodeToString(data, Base64.NO_WRAP);
    }

    private static String unprotect(String stored) throws Exception {
        if (stored.startsWith(PREFIX_PLAIN)) {
            byte[] data = Base64.decode(stored.substring(PREFIX_PLAIN.length()), Base64.NO_WRAP);
            return new String(data, StandardCharsets.UTF_8);
        }
        if (stored.startsWith(PREFIX_ENC) && Build.VERSION.SDK_INT >= 23) {
            return new String(decrypt(stored.substring(PREFIX_ENC.length())), StandardCharsets.UTF_8);
        }
        throw new IllegalStateException("formato desconhecido");
    }

    @TargetApi(23)
    private static String encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getKey());
        byte[] iv = cipher.getIV();
        byte[] enc = cipher.doFinal(data);
        byte[] out = new byte[iv.length + enc.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(enc, 0, out, iv.length, enc.length);
        return Base64.encodeToString(out, Base64.NO_WRAP);
    }

    @TargetApi(23)
    private static byte[] decrypt(String b64) throws Exception {
        byte[] in = Base64.decode(b64, Base64.NO_WRAP);
        if (in.length < 13) {
            throw new IllegalStateException("dado curto");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, in, 0, 12));
        return cipher.doFinal(in, 12, in.length - 12);
    }

    @TargetApi(23)
    private static SecretKey getKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias(KEY_ALIAS)) {
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            kg.generateKey();
        }
        return (SecretKey) ks.getKey(KEY_ALIAS, null);
    }
}

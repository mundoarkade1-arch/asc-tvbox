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
 * Guarda o login no armazenamento privado do app.
 * Os valores são criptografados (AES-GCM) com uma chave do Android Keystore.
 * Se o Keystore do aparelho não funcionar, guarda sem criptografia
 * (continua dentro do armazenamento privado do app).
 *
 * VÁRIAS LISTAS: o app pode ter mais de uma lista salva (arquivo asc_lists, uma peça só,
 * criptografada). Mas o resto do app só enxerga UMA, a selecionada: load() devolve sempre a
 * lista em uso (arquivo asc_login). Trocar a selecionada (select) ou salvar um login novo
 * (save) muda a lista em uso e limpa o que era da lista anterior (Continue assistindo e caches).
 */
public final class CredentialStore {

    public static final String MODE_XTREAM = "xtream";
    public static final String MODE_M3U = "m3u";

    private static final String PREFS = "asc_login";
    private static final String KEY_ALIAS = "asc_tvbox_key";
    private static final String PREFIX_ENC = "K:";
    private static final String PREFIX_PLAIN = "P:";

    /** Dados do login. */
    public static final class Credentials {
        public final String mode;
        public final String server;
        public final String user;
        public final String pass;
        public final String m3uUrl;

        public Credentials(String mode, String server, String user, String pass, String m3uUrl) {
            this.mode = mode;
            this.server = server;
            this.user = user;
            this.pass = pass;
            this.m3uUrl = m3uUrl;
        }
    }

    /** Uma lista salva. "active" = é a lista em uso agora. "number" = posição (1, 2, 3...). */
    public static final class SavedList {
        public final String id;
        public final int number;
        public final boolean active;
        public final Credentials creds;

        SavedList(String id, int number, boolean active, Credentials creds) {
            this.id = id;
            this.number = number;
            this.active = active;
            this.creds = creds;
        }
    }

    private static final String PREFS_LISTS = "asc_lists";
    private static final String KEY_ITEMS = "items";

    private CredentialStore() {
    }

    /**
     * Salva o login e o põe EM USO. Se a lista já existe (mesmo servidor e usuário, ou mesmo
     * link M3U), só atualiza; senão entra na coleção. As outras listas continuam guardadas.
     */
    public static synchronized void save(Context context, Credentials c) {
        Credentials before = load(context);
        writeActive(context, c);
        List<Credentials> all = readAll(context);
        if (before != null && indexOf(all, before) < 0) {
            all.add(before); // login de antes de existirem várias listas: não se perde
        }
        int idx = indexOf(all, c);
        if (idx >= 0) {
            all.set(idx, c);
        } else {
            all.add(c);
        }
        writeAll(context, all);
        if (before == null || !identity(before).equals(identity(c))) {
            clearListCaches(context);
        }
    }

    /** Todas as listas salvas (a em uso vem marcada). Lê o Keystore: não é para laço apertado. */
    public static synchronized List<SavedList> lists(Context context) {
        List<Credentials> all = ensureMigrated(context);
        Credentials active = load(context);
        String activeKey = active == null ? "" : identity(active);
        List<SavedList> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            Credentials c = all.get(i);
            out.add(new SavedList(idOf(c), i + 1, identity(c).equals(activeKey), c));
        }
        return out;
    }

    /** Passa a usar a lista "id". Devolve false se ela não existe mais. */
    public static synchronized boolean select(Context context, String id) {
        List<Credentials> all = ensureMigrated(context);
        for (Credentials c : all) {
            if (idOf(c).equals(id)) {
                Credentials before = load(context);
                writeActive(context, c);
                if (before == null || !identity(before).equals(identity(c))) {
                    clearListCaches(context);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Remove a lista "id". Se era a em uso, passa a usar a primeira que sobrou; se não sobrou
     * nenhuma, o login fica vazio (o app volta para a tela de login).
     */
    public static synchronized void remove(Context context, String id) {
        List<Credentials> all = ensureMigrated(context);
        Credentials active = load(context);
        Credentials removed = null;
        for (int i = 0; i < all.size(); i++) {
            if (idOf(all.get(i)).equals(id)) {
                removed = all.remove(i);
                break;
            }
        }
        if (removed == null) {
            return;
        }
        writeAll(context, all);
        if (active != null && identity(active).equals(identity(removed))) {
            if (all.isEmpty()) {
                clear(context);
            } else {
                writeActive(context, all.get(0));
            }
            clearListCaches(context);
        }
    }

    /**
     * Texto curto para mostrar na tela: tipo e endereço (só o nome do servidor). NUNCA inclui
     * senha nem o link inteiro (o link da lista M3U pode ter senha).
     */
    public static String describe(Credentials c) {
        if (c == null) {
            return "";
        }
        if (MODE_M3U.equals(c.mode)) {
            return "Lista M3U · " + hostOf(c.m3uUrl);
        }
        String u = c.user == null ? "" : c.user.trim();
        return "Servidor · " + hostOf(c.server) + (u.isEmpty() ? "" : " · " + u);
    }

    // ---------- Várias listas: peças internas ----------

    private static String identity(Credentials c) {
        if (MODE_M3U.equals(c.mode)) {
            return "m3u|" + nz(c.m3uUrl).trim();
        }
        return "xt|" + nz(c.server).trim() + "|" + nz(c.user).trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String idOf(Credentials c) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] h = md.digest(identity(c).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", h[i] & 0xFF));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(identity(c).hashCode());
        }
    }

    private static int indexOf(List<Credentials> all, Credentials c) {
        String key = identity(c);
        for (int i = 0; i < all.size(); i++) {
            if (identity(all.get(i)).equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private static String hostOf(String url) {
        String s = nz(url).trim();
        int scheme = s.indexOf("://");
        if (scheme >= 0) {
            s = s.substring(scheme + 3);
        }
        int cut = s.length();
        for (char ch : new char[]{'/', '?', '#'}) {
            int k = s.indexOf(ch);
            if (k >= 0 && k < cut) {
                cut = k;
            }
        }
        s = s.substring(0, cut);
        int at = s.lastIndexOf('@');
        if (at >= 0) {
            s = s.substring(at + 1);
        }
        int colon = s.indexOf(':');
        if (colon >= 0) {
            s = s.substring(0, colon);
        }
        return s.isEmpty() ? "endereço oculto" : s;
    }

    /** Lê a coleção. Se o login em uso ainda não está nela (app antigo), põe e grava. */
    private static List<Credentials> ensureMigrated(Context context) {
        List<Credentials> all = readAll(context);
        Credentials active = load(context);
        if (active != null && indexOf(all, active) < 0) {
            all.add(active);
            writeAll(context, all);
        }
        return all;
    }

    private static List<Credentials> readAll(Context context) {
        List<Credentials> out = new ArrayList<>();
        String stored = context.getApplicationContext()
                .getSharedPreferences(PREFS_LISTS, Context.MODE_PRIVATE).getString(KEY_ITEMS, null);
        if (stored == null || stored.isEmpty()) {
            return out;
        }
        try {
            JSONArray arr = new JSONArray(unprotect(stored));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Credentials(o.optString("mode", MODE_XTREAM),
                        o.optString("server", ""), o.optString("user", ""),
                        o.optString("pass", ""), o.optString("m3u", "")));
            }
        } catch (Exception e) {
            // Chave perdida ou dado corrompido: começa vazio (a lista em uso volta sozinha).
            out.clear();
        }
        return out;
    }

    private static void writeAll(Context context, List<Credentials> all) {
        try {
            JSONArray arr = new JSONArray();
            for (Credentials c : all) {
                JSONObject o = new JSONObject();
                o.put("mode", c.mode);
                o.put("server", nz(c.server));
                o.put("user", nz(c.user));
                o.put("pass", nz(c.pass));
                o.put("m3u", nz(c.m3uUrl));
                arr.put(o);
            }
            context.getApplicationContext().getSharedPreferences(PREFS_LISTS, Context.MODE_PRIVATE)
                    .edit().putString(KEY_ITEMS, protect(arr.toString())).apply();
        } catch (Exception e) {
            // não foi possível guardar a coleção: a lista em uso continua salva do mesmo jeito
        }
    }

    /** Quando a lista em uso muda, o que era da lista anterior não vale mais. */
    private static void clearListCaches(Context context) {
        try {
            ContinueStore.clear(context);
            ImageLoader.clear();
            EpgCache.clear();
        } catch (Exception e) {
            // só cache: se falhar, não atrapalha a troca
        }
    }

    private static void writeActive(Context context, Credentials c) {
        SharedPreferences.Editor ed = prefs(context).edit();
        ed.putString("mode", c.mode);
        ed.putString("server", protect(c.server));
        ed.putString("user", protect(c.user));
        ed.putString("pass", protect(c.pass));
        ed.putString("m3u", protect(c.m3uUrl));
        ed.apply();
    }

    /** Devolve o login salvo, ou null se não houver (ou se não for possível ler). */
    public static Credentials load(Context context) {
        SharedPreferences p = prefs(context);
        String mode = p.getString("mode", null);
        if (mode == null) {
            return null;
        }
        try {
            return new Credentials(
                    mode,
                    unprotect(p.getString("server", "")),
                    unprotect(p.getString("user", "")),
                    unprotect(p.getString("pass", "")),
                    unprotect(p.getString("m3u", "")));
        } catch (Exception e) {
            // Chave perdida ou dado corrompido: apaga e pede o login de novo.
            clear(context);
            return null;
        }
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- Proteção dos valores ----------

    private static String protect(String plain) {
        if (plain == null) {
            plain = "";
        }
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
        if (stored == null || stored.isEmpty()) {
            return "";
        }
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

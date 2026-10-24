package com.ascplay.tvbox;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Guarda o login no armazenamento privado do app.
 * Os valores são criptografados (AES-GCM) com uma chave do Android Keystore.
 * Se o Keystore do aparelho não funcionar, guarda sem criptografia
 * (continua dentro do armazenamento privado do app).
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

    private CredentialStore() {
    }

    public static void save(Context context, Credentials c) {
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

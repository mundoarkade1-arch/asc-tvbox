package com.ascplay.tvbox;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Controle parental: guarda a senha de 4 números e reconhece conteúdo adulto pelo nome.
 *
 * A senha nunca é guardada como texto: só um "resumo embaralhado" (PBKDF2 com sal).
 * Fica num arquivo próprio, separado do login, para o botão "Trocar login" não apagar.
 * Depois de 5 erros seguidos, o app trava por um tempo antes de aceitar nova tentativa.
 * Não registra a senha em log.
 */
public final class ParentalStore {

    public static final int PIN_LENGTH = 4;

    private static final String PREFS = "asc_parental";
    private static final int ITERATIONS = 10000;
    private static final int MAX_FAILS = 5;
    private static final long MAX_LOCK_SECONDS = 3600L;

    // Nome de categoria adulta (mesmas palavras do app original)
    private static final Pattern ADULT_CATEGORY = Pattern.compile(
            "(^|[^a-z0-9])(adult[oa]?s?|xxx|porn[oa]?|erotic[oa]s?|sex|sexy|hentai|playboy"
                    + "|18 ?\\+|\\+ ?18|18plus)($|[^a-z0-9])");

    // Nome de canal ou filme adulto (lista mais curta, para evitar falsos alarmes)
    private static final Pattern ADULT_NAME = Pattern.compile(
            "(^|[^a-z0-9])(xxx|playboy|sextreme|penthouse|brazzers|hustler|porn[oa]?|hentai"
                    + "|sexy ?hot|\\+ ?18|18 ?\\+)($|[^a-z0-9])");

    private ParentalStore() {
    }

    // ---------- Estado ----------

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean("on", false);
    }

    public static boolean isValidPin(String pin) {
        return pin != null && pin.matches("\\d{" + PIN_LENGTH + "}");
    }

    /** Liga o controle parental com a senha dada. Devolve false se a senha for inválida. */
    public static boolean enable(Context context, String pin) {
        if (!isValidPin(pin)) {
            return false;
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hash(pin, salt);
        if (hash == null) {
            return false;
        }
        prefs(context).edit()
                .putBoolean("on", true)
                .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
                .putInt("fails", 0)
                .putLong("locked_until", 0L)
                .apply();
        return true;
    }

    /** Desliga o controle parental e apaga a senha. */
    public static void disable(Context context) {
        prefs(context).edit().clear().apply();
    }

    // ---------- Conferir a senha ----------

    /** Quantos segundos faltam para poder tentar de novo (0 = pode tentar agora). */
    public static long lockedSeconds(Context context) {
        long until = prefs(context).getLong("locked_until", 0L);
        long now = System.currentTimeMillis();
        if (until <= now) {
            return 0L;
        }
        long secs = (until - now + 999L) / 1000L;
        return Math.min(secs, MAX_LOCK_SECONDS);
    }

    /**
     * Confere a senha. Se estiver errada, conta o erro (e trava depois de 5).
     * Se o controle parental estiver desligado, devolve true.
     */
    public static boolean check(Context context, String pin) {
        SharedPreferences p = prefs(context);
        if (!p.getBoolean("on", false)) {
            return true;
        }
        if (lockedSeconds(context) > 0) {
            return false;
        }
        boolean ok = false;
        try {
            String saltText = p.getString("salt", "");
            String hashText = p.getString("hash", "");
            if (isValidPin(pin) && !saltText.isEmpty() && !hashText.isEmpty()) {
                byte[] expected = Base64.decode(hashText, Base64.NO_WRAP);
                byte[] actual = hash(pin, Base64.decode(saltText, Base64.NO_WRAP));
                ok = actual != null && MessageDigest.isEqual(expected, actual);
            }
        } catch (IllegalArgumentException e) {
            ok = false;
        }

        SharedPreferences.Editor ed = p.edit();
        if (ok) {
            ed.putInt("fails", 0);
            ed.putLong("locked_until", 0L);
        } else {
            int fails = p.getInt("fails", 0) + 1;
            ed.putInt("fails", fails);
            if (fails >= MAX_FAILS) {
                long secs = Math.min(MAX_LOCK_SECONDS, 30L << Math.min(fails - MAX_FAILS, 7));
                ed.putLong("locked_until", System.currentTimeMillis() + secs * 1000L);
            }
        }
        ed.apply();
        return ok;
    }

    // ---------- Conteúdo adulto ----------

    /** Nome de categoria parece adulto? */
    public static boolean isAdultCategory(String name) {
        return ADULT_CATEGORY.matcher(simplify(name)).find();
    }

    /** Nome de canal, filme ou série parece adulto? */
    public static boolean isAdultName(String name) {
        return ADULT_NAME.matcher(simplify(name)).find();
    }

    // ---------- Ajudas ----------

    /** Tira acentos e deixa tudo minúsculo, para comparar com as palavras acima. */
    private static String simplify(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static byte[] hash(String pin, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return f.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            return null;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

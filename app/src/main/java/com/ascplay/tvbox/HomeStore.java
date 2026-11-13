package com.ascplay.tvbox;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * Guarda no aparelho dois dados da tela inicial:
 *  - data/hora da última atualização da lista (chip "Lista atualizada");
 *  - último canal ao vivo aberto (card "AO VIVO AGORA").
 *
 * O canal guardado fica criptografado (mesma proteção do ContinueStore), porque na lista M3U
 * o endereço do canal pode ter usuário e senha. Cada dado vale só para a lista (login) atual:
 * "owner" é uma chave embaralhada do login. Nunca registra nada em log.
 */
final class HomeStore {

    private static final String PREFS = "asc_home";
    private static final String KEY_UPDATED = "updated_";
    private static final String KEY_LIVE = "live_";

    private HomeStore() {
    }

    /** Identificador (só texto) do login atual. Não é gravado: só vira chave embaralhada. */
    static String ownerOf(CredentialStore.Credentials c) {
        if (c == null) {
            return "";
        }
        if (CredentialStore.MODE_M3U.equals(c.mode)) {
            return "m3u|" + c.m3uUrl;
        }
        return "xt|" + c.server + "|" + c.user;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- Última atualização ----------

    static void markUpdated(Context c, String owner) {
        prefs(c).edit().putLong(KEY_UPDATED + ContinueStore.keyFor(owner),
                System.currentTimeMillis()).apply();
    }

    /** Milissegundos da última atualização, ou 0 se nunca. */
    static long lastUpdated(Context c, String owner) {
        return prefs(c).getLong(KEY_UPDATED + ContinueStore.keyFor(owner), 0L);
    }

    // ---------- Último canal ao vivo ----------

    static synchronized void saveLastLive(Context c, String owner, XtreamApi.Item item) {
        if (item == null || item.kind != XtreamApi.Kind.LIVE) {
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("i", item.id);
            o.put("n", item.name);
            o.put("c", item.icon);
            o.put("g", item.categoryId);
            o.put("e", item.epgId);
            o.put("x", item.ext);
            o.put("u", item.url);
            prefs(c).edit().putString(KEY_LIVE + ContinueStore.keyFor(owner),
                    ContinueStore.protect(o.toString())).apply();
        } catch (Exception e) {
            // não conseguiu guardar: o card usa o primeiro canal da lista
        }
    }

    /** Último canal aberto, ou null. Lê o Keystore: chamar FORA da thread principal. */
    static synchronized XtreamApi.Item loadLastLive(Context c, String owner) {
        try {
            String stored = prefs(c).getString(KEY_LIVE + ContinueStore.keyFor(owner), null);
            if (stored == null) {
                return null;
            }
            JSONObject o = new JSONObject(ContinueStore.unprotect(stored));
            String id = o.optString("i", "");
            if (id.isEmpty()) {
                return null;
            }
            return new XtreamApi.Item(XtreamApi.Kind.LIVE, id, o.optString("n", ""),
                    o.optString("c", ""), o.optString("g", ""), o.optString("e", ""),
                    o.optString("x", ""), o.optString("u", ""));
        } catch (Exception e) {
            return null;
        }
    }

    /** Apaga tudo (usado se um dia o "Apagar dados" quiser limpar também isto). */
    static void clear(Context c) {
        prefs(c).edit().clear().apply();
    }
}

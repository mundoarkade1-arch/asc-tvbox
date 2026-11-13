package com.ascplay.tvbox;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * "Minha lista": filmes e séries que a pessoa marcou. Guardado só no aparelho
 * (SharedPreferences), TODO criptografado como o ContinueStore (na lista M3U o endereço pode
 * ter senha). No máximo 50 itens, do mais novo ao mais antigo. Uma lista por login.
 *
 * Lê e grava arquivo (e Keystore): chamar FORA da thread principal.
 */
final class FavoritesStore {

    private static final String PREFS = "asc_favorites";
    private static final int MAX_ITEMS = 50;

    private FavoritesStore() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String prefKey(String owner) {
        return "fav_" + ContinueStore.keyFor(owner);
    }

    /** Chave de comparação de um item (tipo + endereço, ou tipo + número). */
    static String identity(XtreamApi.Item item) {
        return item.kind.name() + "|" + (item.url.isEmpty() ? item.id : item.url);
    }

    static synchronized List<XtreamApi.Item> list(Context c, String owner) {
        List<XtreamApi.Item> out = new ArrayList<>();
        try {
            String stored = prefs(c).getString(prefKey(owner), null);
            if (stored == null) {
                return out;
            }
            JSONArray arr = new JSONArray(ContinueStore.unprotect(stored));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                XtreamApi.Kind kind;
                try {
                    kind = XtreamApi.Kind.valueOf(o.optString("k", ""));
                } catch (Exception e) {
                    continue;
                }
                String id = o.optString("i", "");
                if (id.isEmpty()) {
                    continue;
                }
                out.add(new XtreamApi.Item(kind, id, o.optString("n", ""), o.optString("c", ""),
                        o.optString("g", ""), o.optString("e", ""), o.optString("x", ""),
                        o.optString("u", "")));
            }
        } catch (Exception e) {
            // não conseguiu ler: lista vazia
        }
        return out;
    }

    static synchronized boolean contains(Context c, String owner, XtreamApi.Item item) {
        String id = identity(item);
        for (XtreamApi.Item it : list(c, owner)) {
            if (identity(it).equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** Põe o item na lista, ou tira se já estava. Devolve true se AGORA está na lista. */
    static synchronized boolean toggle(Context c, String owner, XtreamApi.Item item) {
        List<XtreamApi.Item> all = list(c, owner);
        String id = identity(item);
        boolean removed = false;
        for (int i = all.size() - 1; i >= 0; i--) {
            if (identity(all.get(i)).equals(id)) {
                all.remove(i);
                removed = true;
            }
        }
        if (!removed) {
            all.add(0, item);
            while (all.size() > MAX_ITEMS) {
                all.remove(all.size() - 1);
            }
        }
        try {
            JSONArray arr = new JSONArray();
            for (XtreamApi.Item it : all) {
                JSONObject o = new JSONObject();
                o.put("k", it.kind.name());
                o.put("i", it.id);
                o.put("n", it.name);
                o.put("c", it.icon);
                o.put("g", it.categoryId);
                o.put("e", it.epgId);
                o.put("x", it.ext);
                o.put("u", it.url);
                arr.put(o);
            }
            prefs(c).edit().putString(prefKey(owner), ContinueStore.protect(arr.toString())).apply();
        } catch (Exception e) {
            return contains(c, owner, item); // não gravou: devolve o que está guardado
        }
        return !removed;
    }
}

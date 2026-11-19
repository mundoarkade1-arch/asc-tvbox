package com.ascplay.tvbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Correção manual do grupo de uma categoria de canais. A correção é guardada pelo NOME da
 * categoria (não pelo servidor): vale também em outros servidores que usem o mesmo nome. Os dados
 * do servidor nunca são alterados. Adulto não pode ser movido (nem para dentro nem para fora do
 * grupo Adultos): quem tranca é o controle parental.
 */
public final class CategoryOverrides {

    private static final String PREFS = "asc_category_groups";
    private static final int MAX_KEY = 120;

    private CategoryOverrides() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(String name) {
        String k = CategoryGroups.simplify(name);
        return k.length() > MAX_KEY ? k.substring(0, MAX_KEY) : k;
    }

    /** Grupo da categoria: a correção manual, se houver; senão o automático. */
    public static CategoryGroups.Group of(Context c, String name) {
        CategoryGroups.Group auto = CategoryGroups.auto(name);
        if (auto == CategoryGroups.Group.ADULTOS) {
            return auto; // correção nunca tira a proteção do adulto
        }
        try {
            int idx = prefs(c).getInt(key(name), -1);
            CategoryGroups.Group[] all = CategoryGroups.Group.values();
            if (idx >= 0 && idx < all.length && all[idx] != CategoryGroups.Group.ADULTOS) {
                return all[idx];
            }
        } catch (Exception ignored) {
            // preferência estranha: usa o automático
        }
        return auto;
    }

    private static void set(Context c, String name, CategoryGroups.Group g) {
        String k = key(name);
        if (k.isEmpty()) {
            return;
        }
        SharedPreferences.Editor ed = prefs(c).edit();
        if (g == null) {
            ed.remove(k);
        } else {
            ed.putInt(k, g.ordinal());
        }
        ed.apply();
    }

    /**
     * Janela "Mover para": a pessoa escolhe o grupo da categoria (ou volta ao automático).
     * Depois de escolher, chama onChanged (na tela) para a lista se reorganizar.
     */
    public static void showPicker(final Activity activity, final String name,
                                  final Runnable onChanged) {
        if (CategoryGroups.auto(name) == CategoryGroups.Group.ADULTOS) {
            return; // adulto não se move
        }
        final CategoryGroups.Group[] all = CategoryGroups.Group.values();
        final CategoryGroups.Group[] choices = new CategoryGroups.Group[all.length - 1];
        String[] labels = new String[all.length]; // grupos (sem Adultos) + "Voltar ao automático"
        int n = 0;
        for (CategoryGroups.Group g : all) {
            if (g == CategoryGroups.Group.ADULTOS) {
                continue;
            }
            choices[n] = g;
            labels[n] = g.label;
            n++;
        }
        labels[n] = "Voltar ao automático";
        final String[] items = labels;
        new AlertDialog.Builder(activity, R.style.AscDialog)
                .setTitle("Mover para (agora: " + of(activity, name).label + ")")
                .setItems(items, (d, which) -> {
                    if (which < choices.length) {
                        set(activity, name, choices[which]);
                    } else {
                        set(activity, name, null);
                    }
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .show();
    }
}

package com.ascplay.tvbox;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.List;
import java.util.TimeZone;

/**
 * Troca semanal das indicações de filmes e séries da tela inicial.
 *
 * Regras:
 *  1) Se o servidor/lista mudou (os 10 primeiros itens ficaram diferentes do que o app viu
 *     da última vez), mostra os 10 primeiros NA HORA: é o conteúdo novo.
 *  2) Se não mudou, a cada segunda-feira (a partir da meia-noite) o app passa para os 10 itens
 *     seguintes da lista (10 a 19, depois 20 a 29...). Depois de 20 janelas (os 200 primeiros)
 *     volta ao começo.
 *  3) Durante a semana a janela fica fixa. O botão "Atualizar" não troca de janela.
 *
 * Guarda só números e os ids/nomes dos 10 primeiros (uma "assinatura"), nada de endereço.
 * Cada login (owner) e cada tipo (filmes, séries) tem a sua própria contagem.
 */
final class WeeklyRotation {

    static final int WINDOW = 10;
    static final int MAX_WINDOWS = 20;

    private static final String PREFS = "asc_rotation";

    private WeeklyRotation() {
    }

    /** Número da semana, contando de segunda a domingo, no fuso do aparelho. */
    static long currentWeek() {
        long now = System.currentTimeMillis();
        long local = now + TimeZone.getDefault().getOffset(now);
        long days = Math.floorDiv(local, 86400000L); // dias desde 01/01/1970 (quinta-feira)
        return Math.floorDiv(days + 3L, 7L);          // +3 faz a semana começar na segunda
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(String owner, XtreamApi.Kind kind) {
        return "rot_" + ContinueStore.keyFor(owner) + "_" + kind.name();
    }

    private static String signature(List<XtreamApi.Item> head) {
        StringBuilder sb = new StringBuilder();
        for (XtreamApi.Item it : head) {
            sb.append(it.id).append(':').append(it.name).append('|');
        }
        return sb.toString();
    }

    /**
     * Diz qual janela mostrar agora (0 = os 10 primeiros). "head" são os 10 primeiros itens
     * atuais do servidor. Já guarda a decisão.
     */
    static synchronized int decide(Context c, String owner, XtreamApi.Kind kind,
                                   List<XtreamApi.Item> head) {
        String sig = signature(head);
        String savedSig = "";
        long savedWeek = -1L;
        int index = 0;
        try {
            String raw = prefs(c).getString(key(owner, kind), null);
            if (raw != null) {
                JSONObject o = new JSONObject(raw);
                savedSig = o.optString("s", "");
                savedWeek = o.optLong("w", -1L);
                index = o.optInt("i", 0);
            }
        } catch (Exception e) {
            savedSig = "";
        }

        long week = currentWeek();
        if (savedSig.isEmpty() || !savedSig.equals(sig)) {
            index = 0;            // conteúdo novo no começo da lista (ou primeira vez)
            savedWeek = week;
        } else if (savedWeek != week) {
            index = (index + 1) % MAX_WINDOWS; // virou a semana: próxima janela
            savedWeek = week;
        }
        write(c, owner, kind, sig, savedWeek, index);
        return index;
    }

    /** A janela escolhida veio vazia (lista curta): volta ao começo. */
    static synchronized void reset(Context c, String owner, XtreamApi.Kind kind,
                                   List<XtreamApi.Item> head) {
        write(c, owner, kind, signature(head), currentWeek(), 0);
    }

    private static void write(Context c, String owner, XtreamApi.Kind kind, String sig,
                              long week, int index) {
        try {
            JSONObject o = new JSONObject();
            o.put("s", sig);
            o.put("w", week);
            o.put("i", index);
            prefs(c).edit().putString(key(owner, kind), o.toString()).apply();
        } catch (Exception e) {
            // não conseguiu guardar: mostra do mesmo jeito; na próxima abertura tenta de novo
        }
    }
}

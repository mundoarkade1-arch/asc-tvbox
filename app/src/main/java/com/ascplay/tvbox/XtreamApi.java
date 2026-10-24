package com.ascplay.tvbox;

import android.net.Uri;
import android.util.Base64;
import android.util.JsonReader;
import android.util.JsonToken;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Busca categorias e itens (canais, filmes, séries) no servidor Xtream.
 *
 * Economia de memória: a resposta é lida aos poucos (streaming) e a leitura
 * PARA assim que o limite de itens é atingido. A lista inteira nunca fica na memória.
 *
 * Faz acesso à rede: NUNCA chamar na thread principal.
 * Não registra senhas nem endereços em log.
 */
public final class XtreamApi {

    public enum Kind { LIVE, MOVIE, SERIES }

    public static final class Category {
        public final String id;
        public final String name;

        Category(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static final class Item {
        public final Kind kind;
        public final String id;
        public final String name;
        public final String icon;
        public final String categoryId;
        public final String epgId;
        public final String ext;

        Item(Kind kind, String id, String name, String icon, String categoryId,
             String epgId, String ext) {
            this.kind = kind;
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.categoryId = categoryId;
            this.epgId = epgId;
            this.ext = ext;
        }
    }

    /**
     * Programação de um canal: o que passa agora e o que vem a seguir.
     * Horários em milissegundos (como System.currentTimeMillis()).
     * nextTitle fica vazio e nextStart fica 0 quando não há "a seguir".
     */
    public static final class Epg {
        public final String title;
        public final long start;
        public final long end;
        public final String nextTitle;
        public final long nextStart;

        Epg(String title, long start, long end, String nextTitle, long nextStart) {
            this.title = title;
            this.start = start;
            this.end = end;
            this.nextTitle = nextTitle;
            this.nextStart = nextStart;
        }
    }

    private static final class Slot {
        final String title;
        final long start;
        final long end;

        Slot(String title, long start, long end) {
            this.title = title;
            this.start = start;
            this.end = end;
        }
    }

    private static final int TIMEOUT_MS = 15000;
    private static final int MAX_REDIRECTS = 5;
    private static final long MAX_BYTES = 80L * 1024L * 1024L;
    private static final int MAX_CATEGORIES = 3000;
    private static final int MAX_NAME = 200;
    private static final int MAX_EPG_SLOTS = 8;
    private static final String USER_AGENT = "ASC-TVBox/1.0 (Android)";

    private XtreamApi() {
    }

    // ---------- Chamadas públicas ----------

    public static List<Category> categories(String server, String user, String pass, Kind kind)
            throws IOException {
        String url = apiUrl(server, user, pass, categoriesAction(kind));
        HttpURLConnection conn = open(url);
        try {
            checkOk(conn);
            JsonReader r = reader(conn);
            try {
                List<Category> out = new ArrayList<>();
                if (r.peek() != JsonToken.BEGIN_ARRAY) {
                    throw new IOException("resposta invalida");
                }
                r.beginArray();
                while (r.hasNext() && out.size() < MAX_CATEGORIES) {
                    if (r.peek() != JsonToken.BEGIN_OBJECT) {
                        r.skipValue();
                        continue;
                    }
                    String id = "";
                    String name = "";
                    r.beginObject();
                    while (r.hasNext()) {
                        String key = r.nextName();
                        if ("category_id".equals(key)) {
                            id = readText(r);
                        } else if ("category_name".equals(key)) {
                            name = readText(r);
                        } else {
                            r.skipValue();
                        }
                    }
                    r.endObject();
                    if (isNumber(id) && !name.isEmpty()) {
                        out.add(new Category(id, cut(name)));
                    }
                }
                return out;
            } finally {
                closeQuietly(r);
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Devolve até "limit" itens da categoria. Se categoryId for null, pega os primeiros de qualquer categoria.
     * Para de ler a resposta assim que atinge o limite.
     */
    public static List<Item> items(String server, String user, String pass, Kind kind,
                                   String categoryId, int limit) throws IOException {
        return items(server, user, pass, kind, categoryId, 0, limit);
    }

    /**
     * Igual ao anterior, mas pula os primeiros "offset" itens (para carregar por páginas).
     * Os itens pulados são lidos e descartados na hora, sem ficar na memória.
     */
    public static List<Item> items(String server, String user, String pass, Kind kind,
                                   String categoryId, int offset, int limit) throws IOException {
        String url = apiUrl(server, user, pass, itemsAction(kind));
        if (categoryId != null && isNumber(categoryId)) {
            url += "&category_id=" + categoryId;
        }
        HttpURLConnection conn = open(url);
        try {
            checkOk(conn);
            JsonReader r = reader(conn);
            try {
                List<Item> out = new ArrayList<>();
                if (r.peek() != JsonToken.BEGIN_ARRAY) {
                    throw new IOException("resposta invalida");
                }
                r.beginArray();
                int skipped = 0;
                while (r.hasNext() && out.size() < limit) {
                    if (r.peek() != JsonToken.BEGIN_OBJECT) {
                        r.skipValue();
                        continue;
                    }
                    Item item = readItem(r, kind);
                    if (item == null) {
                        continue;
                    }
                    // Alguns servidores ignoram o filtro de categoria: conferimos aqui.
                    if (categoryId != null && !categoryId.equals(item.categoryId)) {
                        continue;
                    }
                    if (skipped < offset) {
                        skipped++;
                        continue;
                    }
                    out.add(item);
                }
                return out;
            } finally {
                closeQuietly(r);
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Programação de UM canal (o que passa agora e o que vem a seguir).
     * Devolve null se o servidor não tem guia para o canal ou se nada passa agora.
     * Pede poucos programas por vez, nunca o guia inteiro.
     */
    public static Epg epg(String server, String user, String pass, String streamId)
            throws IOException {
        if (!isNumber(streamId)) {
            return null;
        }
        String url = apiUrl(server, user, pass, "get_short_epg")
                + "&stream_id=" + streamId + "&limit=4";
        HttpURLConnection conn = open(url);
        try {
            checkOk(conn);
            JsonReader r = reader(conn);
            try {
                List<Slot> slots = new ArrayList<>();
                if (r.peek() != JsonToken.BEGIN_OBJECT) {
                    // Sem guia: alguns servidores devolvem uma lista vazia
                    return null;
                }
                r.beginObject();
                while (r.hasNext()) {
                    String key = r.nextName();
                    if ("epg_listings".equals(key) && r.peek() == JsonToken.BEGIN_ARRAY) {
                        r.beginArray();
                        while (r.hasNext() && slots.size() < MAX_EPG_SLOTS) {
                            if (r.peek() != JsonToken.BEGIN_OBJECT) {
                                r.skipValue();
                                continue;
                            }
                            Slot s = readSlot(r);
                            if (s != null) {
                                slots.add(s);
                            }
                        }
                        while (r.hasNext()) {
                            r.skipValue();
                        }
                        r.endArray();
                    } else {
                        r.skipValue();
                    }
                }
                r.endObject();

                long now = System.currentTimeMillis();
                for (int i = 0; i < slots.size(); i++) {
                    Slot s = slots.get(i);
                    if (s.start <= now && now < s.end) {
                        if (i + 1 < slots.size()) {

package com.ascplay.tvbox;

import android.net.Uri;
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

    private static final int TIMEOUT_MS = 15000;
    private static final int MAX_REDIRECTS = 5;
    private static final long MAX_BYTES = 80L * 1024L * 1024L;
    private static final int MAX_CATEGORIES = 3000;
    private static final int MAX_NAME = 200;
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

    /** Endereço para tocar um canal ou filme. Séries precisam de episódios (etapa futura). */
    public static String streamUrl(String server, String user, String pass, Item item) {
        String base = server + "/";
        String u = Uri.encode(user) + "/" + Uri.encode(pass) + "/";
        if (item.kind == Kind.LIVE) {
            return base + "live/" + u + item.id + ".m3u8";
        }
        if (item.kind == Kind.MOVIE) {
            String ext = item.ext.isEmpty() ? "mp4" : item.ext;
            return base + "movie/" + u + item.id + "." + ext;
        }
        return null;
    }

    // ---------- Leitura de um item ----------

    private static Item readItem(JsonReader r, Kind kind) throws IOException {
        String id = "";
        String name = "";
        String icon = "";
        String cat = "";
        String epg = "";
        String ext = "";
        r.beginObject();
        while (r.hasNext()) {
            String key = r.nextName();
            switch (key) {
                case "stream_id":
                    if (kind != Kind.SERIES) {
                        id = readText(r);
                    } else {
                        r.skipValue();
                    }
                    break;
                case "series_id":
                    if (kind == Kind.SERIES) {
                        id = readText(r);
                    } else {
                        r.skipValue();
                    }
                    break;
                case "name":
                    name = readText(r);
                    break;
                case "stream_icon":
                case "cover":
                    String v = readText(r);
                    if (icon.isEmpty()) {
                        icon = v;
                    }
                    break;
                case "category_id":
                    cat = readText(r);
                    break;
                case "epg_channel_id":
                    epg = readText(r);
                    break;
                case "container_extension":
                    ext = readText(r);
                    break;
                default:
                    r.skipValue();
                    break;
            }
        }
        r.endObject();

        if (!isNumber(id) || name.isEmpty()) {
            return null;
        }
        String lowerIcon = icon.toLowerCase();
        if (!lowerIcon.startsWith("http://") && !lowerIcon.startsWith("https://")) {
            icon = "";
        }
        if (!ext.matches("[A-Za-z0-9]{1,5}")) {
            ext = "";
        }
        return new Item(kind, id, cut(name), icon, cat, epg.trim(), ext);
    }

    // ---------- Ajudas ----------

    private static String categoriesAction(Kind kind) {
        switch (kind) {
            case MOVIE:
                return "get_vod_categories";
            case SERIES:
                return "get_series_categories";
            default:
                return "get_live_categories";
        }
    }

    private static String itemsAction(Kind kind) {
        switch (kind) {
            case MOVIE:
                return "get_vod_streams";
            case SERIES:
                return "get_series";
            default:
                return "get_live_streams";
        }
    }

    private static String apiUrl(String server, String user, String pass, String action)
            throws IOException {
        try {
            return server + "/player_api.php?username=" + URLEncoder.encode(user, "UTF-8")
                    + "&password=" + URLEncoder.encode(pass, "UTF-8")
                    + "&action=" + action;
        } catch (Exception e) {
            throw new IOException("endereco");
        }
    }

    /** Lê texto, número ou nulo como texto. Qualquer outra coisa é ignorada. */
    private static String readText(JsonReader r) throws IOException {
        JsonToken t = r.peek();
        if (t == JsonToken.STRING || t == JsonToken.NUMBER) {
            return r.nextString().trim();
        }
        if (t == JsonToken.NULL) {
            r.nextNull();
            return "";
        }
        r.skipValue();
        return "";
    }

    private static boolean isNumber(String s) {
        return s != null && s.matches("\\d{1,12}");
    }

    private static String cut(String s) {
        return s.length() > MAX_NAME ? s.substring(0, MAX_NAME) : s;
    }

    private static void checkOk(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("resposta " + code);
        }
    }

    private static JsonReader reader(HttpURLConnection conn) throws IOException {
        InputStream in = new BufferedInputStream(new LimitedStream(conn.getInputStream(), MAX_BYTES));
        JsonReader r = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        r.setLenient(true);
        return r;
    }

    private static void closeQuietly(JsonReader r) {
        try {
            r.close();
        } catch (IOException ignored) {
            // nada a fazer
        }
    }

    /** Abre o endereço seguindo redirecionamentos (inclusive de http para https). */
    private static HttpURLConnection open(String address) throws IOException {
        URL url = new URL(address);
        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isEmpty()) {
                    throw new IOException("redirecionamento sem destino");
                }
                url = new URL(url, location);
                String protocol = url.getProtocol();
                if (!"http".equals(protocol) && !"https".equals(protocol)) {
                    throw new IOException("redirecionamento invalido");
                }
                continue;
            }
            return conn;
        }
        throw new IOException("redirecionamentos demais");
    }

    /** Corta a leitura se a resposta passar do limite de bytes. */
    private static final class LimitedStream extends FilterInputStream {
        private long left;

        LimitedStream(InputStream in, long max) {
            super(in);
            this.left = max;
        }

        @Override
        public int read() throws IOException {
            if (left <= 0) {
                throw new IOException("resposta grande demais");
            }
            int b = super.read();
            if (b >= 0) {
                left--;
            }
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            if (left <= 0) {
                throw new IOException("resposta grande demais");
            }
            int n = super.read(buf, off, (int) Math.min(len, left));
            if (n > 0) {
                left -= n;
            }
            return n;
        }
    }
}

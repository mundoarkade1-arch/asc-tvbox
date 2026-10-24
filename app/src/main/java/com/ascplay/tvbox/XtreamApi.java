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

    /** EPISODE é um episódio de série (não aparece em listas, só serve para tocar). */
    public enum Kind { LIVE, MOVIE, SERIES, EPISODE }

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

    /**
     * Um episódio de série. "season" é a temporada e "number" o número do episódio
     * (0 quando o servidor não informa). "title" pode vir vazio.
     */
    public static final class Episode {
        public final String id;
        public final String title;
        public final int season;
        public final int number;
        public final String ext;

        Episode(String id, String title, int season, int number, String ext) {
            this.id = id;
            this.title = title;
            this.season = season;
            this.number = number;
            this.ext = ext;
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
    private static final int MAX_EPISODES = 500;
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
                            Slot n = slots.get(i + 1);
                            return new Epg(s.title, s.start, s.end, n.title, n.start);
                        }
                        return new Epg(s.title, s.start, s.end, "", 0L);
                    }
                }
                return null;
            } finally {
                closeQuietly(r);
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Episódios de UMA série (get_series_info). Devolve no máximo 500, na ordem em que
     * o servidor mandou. A resposta é lida aos poucos e a leitura para ao chegar no limite.
     * Devolve lista vazia se a série não tem episódios.
     */
    public static List<Episode> episodes(String server, String user, String pass, String seriesId)
            throws IOException {
        List<Episode> out = new ArrayList<>();
        if (!isNumber(seriesId)) {
            return out;
        }
        String url = apiUrl(server, user, pass, "get_series_info") + "&series_id=" + seriesId;
        HttpURLConnection conn = open(url);
        try {
            checkOk(conn);
            JsonReader r = reader(conn);
            try {
                if (r.peek() != JsonToken.BEGIN_OBJECT) {
                    throw new IOException("resposta invalida");
                }
                r.beginObject();
                while (r.hasNext()) {
                    String key = r.nextName();
                    if ("episodes".equals(key)) {
                        if (readEpisodeGroups(r, out)) {
                            return out; // chegou no limite: para de ler
                        }
                    } else {
                        r.skipValue();
                    }
                }
                r.endObject();
                return out;
            } finally {
                closeQuietly(r);
            }
        } finally {
            conn.disconnect();
        }
    }

    /** Endereço para tocar um canal, filme ou episódio. Séries inteiras precisam dos episódios. */
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
        if (item.kind == Kind.EPISODE) {
            String ext = item.ext.isEmpty() ? "mp4" : item.ext;
            return base + "series/" + u + item.id + "." + ext;
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

    /** Lê um programa do guia. Devolve null se faltar título ou horário válido. */
    private static Slot readSlot(JsonReader r) throws IOException {
        String title = "";
        String start = "";
        String stop = "";
        r.beginObject();
        while (r.hasNext()) {
            String key = r.nextName();
            if ("title".equals(key)) {
                title = readText(r);
            } else if ("start_timestamp".equals(key)) {
                start = readText(r);
            } else if ("stop_timestamp".equals(key)) {
                stop = readText(r);
            } else {
                r.skipValue();
            }
        }
        r.endObject();

        long s = parseSeconds(start);
        long e = parseSeconds(stop);
        String t = decodeTitle(title);
        if (t.isEmpty() || s <= 0 || e <= s) {
            return null;
        }
        return new Slot(cut(t), s * 1000L, e * 1000L);
    }

    // ---------- Leitura de episódios ----------

    /**
     * Lê o campo "episodes". O servidor manda um objeto com uma lista por temporada
     * ("1": [...], "2": [...]), ou uma lista de listas. Devolve true se chegou no limite.
     */
    private static boolean readEpisodeGroups(JsonReader r, List<Episode> out) throws IOException {
        JsonToken t = r.peek();
        if (t == JsonToken.BEGIN_OBJECT) {
            r.beginObject();
            while (r.hasNext()) {
                int season = parseIntSafe(r.nextName());
                if (r.peek() == JsonToken.BEGIN_ARRAY) {
                    if (readEpisodeList(r, season, out)) {
                        return true;
                    }
                } else {
                    r.skipValue();
                }
            }
            r.endObject();
        } else if (t == JsonToken.BEGIN_ARRAY) {
            r.beginArray();
            int index = 0;
            while (r.hasNext()) {
                index++;
                if (r.peek() == JsonToken.BEGIN_ARRAY) {
                    if (readEpisodeList(r, index, out)) {
                        return true;
                    }
                } else {
                    r.skipValue();
                }
            }
            r.endArray();
        } else {
            r.skipValue();
        }
        return false;
    }

    /** Lê a lista de episódios de uma temporada. Devolve true se chegou no limite. */
    private static boolean readEpisodeList(JsonReader r, int season, List<Episode> out)
            throws IOException {
        r.beginArray();
        while (r.hasNext()) {
            if (out.size() >= MAX_EPISODES) {
                return true;
            }
            if (r.peek() != JsonToken.BEGIN_OBJECT) {
                r.skipValue();
                continue;
            }
            Episode e = readEpisode(r, season);
            if (e != null) {
                out.add(e);
            }
        }
        r.endArray();
        return false;
    }

    /** Lê um episódio. Devolve null se não tiver número de identificação válido. */
    private static Episode readEpisode(JsonReader r, int fallbackSeason) throws IOException {
        String id = "";
        String title = "";
        String num = "";
        String seasonText = "";
        String ext = "";
        r.beginObject();
        while (r.hasNext()) {
            String key = r.nextName();
            switch (key) {
                case "id":
                    id = readText(r);
                    break;
                case "title":
                    title = readText(r);
                    break;
                case "episode_num":
                    num = readText(r);
                    break;
                case "season":
                    seasonText = readText(r);
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

        if (!isNumber(id)) {
            return null;
        }
        if (!ext.matches("[A-Za-z0-9]{1,5}")) {
            ext = "";
        }
        int season = isNumber(seasonText) ? parseIntSafe(seasonText) : fallbackSeason;
        int number = parseIntSafe(num);
        return new Episode(id, cut(title), season, number, ext);
    }

    private static int parseIntSafe(String s) {
        if (!isNumber(s)) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static long parseSeconds(String s) {
        if (!isNumber(s)) {
            return 0L;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    /** O título do guia costuma vir em Base64. Se não for, usa o texto como veio. */
    private static String decodeTitle(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        try {
            byte[] bytes = Base64.decode(raw, Base64.DEFAULT);
            String s = new String(bytes, StandardCharsets.UTF_8).trim();
            if (!s.isEmpty() && looksLikeText(s)) {
                return s;
            }
        } catch (IllegalArgumentException ignored) {
            // não era Base64: usa o texto original
        }
        return raw.trim();
    }

    private static boolean looksLikeText(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\uFFFD' || (c < 32 && c != '\n' && c != '\t' && c != '\r')) {
                return false;
            }
        }
        return true;
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

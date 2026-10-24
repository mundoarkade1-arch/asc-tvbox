package com.ascplay.tvbox;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê uma lista M3U e separa canais ao vivo, filmes e séries.
 *
 * Economia de memória: a lista é lida aos poucos (linha por linha) e a leitura PARA
 * assim que cada tipo atinge o limite pedido. A lista inteira nunca fica na memória.
 * Para listas gigantes, a leitura também para depois de 200 MB ou de 1 minuto e devolve
 * o que já achou.
 *
 * Faz acesso à rede: NUNCA chamar na thread principal.
 * Os endereços dos vídeos costumam conter usuário e senha: nunca mostrar nem registrar em log.
 */
public final class M3uApi {

    /** Primeiros itens de cada tipo, na ordem em que aparecem na lista. */
    public static final class Sample {
        public final List<XtreamApi.Item> live = new ArrayList<>();
        public final List<XtreamApi.Item> movie = new ArrayList<>();
        public final List<XtreamApi.Item> series = new ArrayList<>();
    }

    private static final int TIMEOUT_MS = 15000;
    private static final int MAX_REDIRECTS = 5;
    private static final long MAX_BYTES = 200L * 1024L * 1024L;
    private static final long MAX_SECONDS = 60L;
    private static final int MAX_LINE = 4096;
    private static final int MAX_NAME = 200;
    private static final String USER_AGENT = "ASC-TVBox/1.0 (Android)";

    private static final Pattern ATTR = Pattern.compile("([\\w-]+)=\"([^\"]*)\"");
    private static final Pattern EP = Pattern.compile(
            "\\bS\\d{1,2}\\s?E\\d{1,3}\\b|\\bT\\d{1,2}\\s?E\\d{1,3}\\b|\\b\\d{1,2}x\\d{2,3}\\b|temporada",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VIDEO_EXT = Pattern.compile(
            "\\.(mp4|mkv|avi|mov|wmv|flv|mpg|mpeg|webm)$");
    private static final Pattern LIVE_EXT = Pattern.compile("\\.(m3u8|ts)$");
    private static final Pattern G_SERIES = Pattern.compile(
            "s[e\u00e9]ries?|novelas?|animes?|doramas?|temporada");
    private static final Pattern G_LIVE = Pattern.compile(
            "canais|canal|24\\s?h|ao vivo|live|\\btv\\b|esporte|abertos|not[i\u00ed]cias");
    private static final Pattern G_MOVIE = Pattern.compile(
            "filmes?|movies?|\\bvod\\b|cine|lan[c\u00e7]amentos?");
    private static final Pattern SERIES_TITLE = Pattern.compile(
            "\\s*[-\u2013]?\\s*[ST]\\d{1,2}\\s?E\\d{1,3}.*$|\\s*[-\u2013]?\\s*\\d{1,2}x\\d{2,3}.*$",
            Pattern.CASE_INSENSITIVE);

    private M3uApi() {
    }

    // ---------- Chamada pública ----------

    /**
     * Devolve até "limit" itens de cada tipo (canais, filmes e séries), lendo a lista só até
     * achar o suficiente. Séries são agrupadas pelo título (sem "S01E03"): um item por série.
     * Se hideAdult for verdadeiro, itens adultos (pelo nome ou pela categoria) são pulados.
     */
    public static Sample sample(String m3uUrl, int limit, boolean hideAdult) throws IOException {
        if (m3uUrl == null || !isHttp(m3uUrl.trim())) {
            throw new IOException("endereco");
        }
        Sample out = new Sample();
        HttpURLConnection conn = open(m3uUrl.trim());
        try {
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IOException("resposta " + code);
            }
            Reader reader = new InputStreamReader(
                    new LimitedStream(conn.getInputStream(), MAX_BYTES), StandardCharsets.UTF_8);
            try {
                scan(reader, out, limit, hideAdult);
            } finally {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    // nada a fazer
                }
            }
        } catch (IOException e) {
            // Lista enorme ou conexão que caiu no meio: se já achamos algo, mostra o que deu.
            if (out.live.isEmpty() && out.movie.isEmpty() && out.series.isEmpty()) {
                throw e;
            }
        } finally {
            conn.disconnect();
        }
        return out;
    }

    // ---------- Leitura da lista ----------

    private static void scan(Reader reader, Sample out, int limit, boolean hideAdult)
            throws IOException {
        long deadline = System.nanoTime() + MAX_SECONDS * 1000000000L;
        Lines lines = new Lines(reader);
        StringBuilder sb = new StringBuilder();
        HashSet<String> seenSeries = new HashSet<>();

        boolean have = false;
        String name = "";
        String logo = "";
        String group = "";
        String tvgId = "";
        int counter = 0;
        int count = 0;

        while (lines.next(sb)) {
            if ((++count & 1023) == 0 && System.nanoTime() > deadline) {
                return;
            }
            String line = sb.toString().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("#EXTINF")) {
                int comma = line.lastIndexOf(',');
                String head = comma > 0 ? line.substring(0, comma) : line;
                String title = comma >= 0 ? line.substring(comma + 1).trim() : "";
                String tvgName = "";
                logo = "";
                group = "";
                tvgId = "";
                Matcher m = ATTR.matcher(head);
                while (m.find()) {
                    String key = m.group(1).toLowerCase(Locale.ROOT);
                    String value = m.group(2).trim();
                    if ("tvg-logo".equals(key)) {
                        logo = value;
                    } else if ("group-title".equals(key)) {
                        group = value;
                    } else if ("tvg-id".equals(key)) {
                        tvgId = value;
                    } else if ("tvg-name".equals(key)) {
                        tvgName = value;
                    }
                }
                name = title.isEmpty() ? tvgName : title;
                if (group.isEmpty()) {
                    group = "Outros";
                }
                have = true;
                continue;
            }
            if (line.charAt(0) == '#' || !have) {
                continue;
            }
            have = false;
            if (!isHttp(line) || name.isEmpty()) {
                continue;
            }
            counter++;

            XtreamApi.Kind kind = classify(line, group, name);
            List<XtreamApi.Item> target;
            if (kind == XtreamApi.Kind.LIVE) {
                target = out.live;
            } else if (kind == XtreamApi.Kind.MOVIE) {
                target = out.movie;
            } else {
                target = out.series;
            }
            if (target.size() >= limit) {
                continue;
            }
            if (hideAdult && (ParentalStore.isAdultCategory(group)
                    || ParentalStore.isAdultName(name))) {
                continue;
            }
            String icon = isHttp(logo) ? logo : "";

            if (kind == XtreamApi.Kind.SERIES) {
                String seriesName = seriesTitle(name);
                if (!seenSeries.add(seriesName.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                // Série: sem endereço próprio. Os episódios são buscados na lista quando a série abrir.
                target.add(new XtreamApi.Item(kind, String.valueOf(counter), cut(seriesName),
                        icon, group, "", ""));
            } else {
                target.add(new XtreamApi.Item(kind, String.valueOf(counter), cut(name),
                        icon, group, tvgId, "", line));
            }

            if (out.live.size() >= limit && out.movie.size() >= limit
                    && out.series.size() >= limit) {
                return;
            }
        }
    }

    /** Decide se o item é canal ao vivo, filme ou série (mesmas regras do app original). */
    static XtreamApi.Kind classify(String url, String group, String name) {
        String path = url;
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        path = path.toLowerCase(Locale.ROOT);
        String g = group.toLowerCase(Locale.ROOT);

        if (path.contains("/series/")) {
            return XtreamApi.Kind.SERIES;
        }
        if (path.contains("/movie/")) {
            return XtreamApi.Kind.MOVIE;
        }
        if (path.contains("/live/")) {
            return XtreamApi.Kind.LIVE;
        }
        boolean episodeLike = EP.matcher(name).find() || G_SERIES.matcher(g).find();
        if (VIDEO_EXT.matcher(path).find()) {
            return episodeLike ? XtreamApi.Kind.SERIES : XtreamApi.Kind.MOVIE;
        }
        if (LIVE_EXT.matcher(path).find()) {
            return XtreamApi.Kind.LIVE;
        }
        if (G_LIVE.matcher(g).find()) {
            return XtreamApi.Kind.LIVE;
        }
        if (episodeLike) {
            return XtreamApi.Kind.SERIES;
        }
        if (G_MOVIE.matcher(g).find()) {
            return XtreamApi.Kind.MOVIE;
        }
        return XtreamApi.Kind.LIVE;
    }

    /** Tira "S01E03", "T1 E5" ou "1x05" do fim do nome, deixando só o título da série. */
    static String seriesTitle(String name) {
        String t = SERIES_TITLE.matcher(name).replaceFirst("").trim();
        return t.isEmpty() ? name : t;
    }

    // ---------- Ajudas ----------

    private static boolean isHttp(String s) {
        return s != null && (s.regionMatches(true, 0, "http://", 0, 7)
                || s.regionMatches(true, 0, "https://", 0, 8));
    }

    private static String cut(String s) {
        return s.length() > MAX_NAME ? s.substring(0, MAX_NAME) : s;
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

    /** Lê linha por linha. Linhas maiores que o limite são cortadas (o resto é descartado). */
    private static final class Lines {
        private final Reader reader;
        private final char[] buf = new char[8192];
        private int len = 0;
        private int pos = 0;

        Lines(Reader reader) {
            this.reader = reader;
        }

        /** Põe a próxima linha em sb. Devolve false quando a lista acabou. */
        boolean next(StringBuilder sb) throws IOException {
            sb.setLength(0);
            boolean any = false;
            while (true) {
                if (pos >= len) {
                    len = reader.read(buf, 0, buf.length);
                    pos = 0;
                    if (len < 0) {
                        len = 0;
                        return any;
                    }
                    if (len == 0) {
                        continue;
                    }
                }
                while (pos < len) {
                    char c = buf[pos++];
                    any = true;
                    if (c == '\n') {
                        return true;
                    }
                    if (c != '\r' && sb.length() < MAX_LINE) {
                        sb.append(c);
                    }
                }
            }
        }
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

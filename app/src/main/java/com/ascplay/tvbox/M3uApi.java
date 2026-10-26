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
 * assim que acha o que foi pedido. A lista inteira nunca fica na memória.
 * Para listas gigantes, a leitura também para depois de 200 MB ou de 1 minuto e devolve
 * o que já achou.
 *
 * Funções públicas:
 * - sample: os primeiros itens de cada tipo (página inicial).
 * - categories: os nomes das categorias de um tipo ("Ver mais").
 * - page: uma página de itens de um tipo, de uma categoria ou de todas ("Ver mais").
 * - episodes: os episódios de UMA série, achados pelo título (tela da série).
 * - search: os itens de um tipo cujo nome combina com o que a pessoa digitou (busca).
 * - searchStream: igual à search, mas avisa cada resultado assim que o acha (M11b).
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

    /**
     * Um episódio de série da lista M3U. "season" é a temporada e "number" o número do
     * episódio (1 e 0 quando o nome não diz). "url" é o endereço direto do vídeo e costuma
     * conter usuário e senha: nunca mostrar nem registrar em log.
     */
    public static final class Episode {
        public final String title;
        public final int season;
        public final int number;
        public final String url;

        Episode(String title, int season, int number, String url) {
            this.title = title;
            this.season = season;
            this.number = number;
            this.url = url;
        }
    }

    /** Recebe cada item da lista. Devolve false para parar a leitura. */
    private interface Visitor {
        boolean entry(String url, String group, String name, String icon, String tvgId,
                      int counter);
    }

    /** Uma tarefa que lê a lista já aberta. */
    private interface Job {
        void run(Reader reader) throws IOException;
    }

    private static final int TIMEOUT_MS = 15000;
    private static final int MAX_REDIRECTS = 5;
    private static final long MAX_BYTES = 200L * 1024L * 1024L;
    private static final long MAX_SECONDS = 60L;
    private static final int MAX_LINE = 4096;
    private static final int MAX_NAME = 200;
    private static final int MAX_CATEGORIES = 3000;
    private static final int MAX_EPISODES = 500;
    private static final String USER_AGENT = "ASC-TVBox/1.0 (Android)";

    private static final Pattern ATTR = Pattern.compile("([\\w-]+)=\"([^\"]*)\"");
    private static final Pattern EP = Pattern.compile(
            "\\bS\\d{1,2}\\s?E\\d{1,3}\\b|\\bT\\d{1,2}\\s?E\\d{1,3}\\b|\\b\\d{1,2}x\\d{2,3}\\b|temporada",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SEASON_EP = Pattern.compile(
            "\\b[ST](\\d{1,2})\\s?E(\\d{1,3})\\b|\\b(\\d{1,2})x(\\d{2,3})\\b",
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

    // ---------- Chamadas públicas ----------

    /**
     * Devolve até "limit" itens de cada tipo (canais, filmes e séries), lendo a lista só até
     * achar o suficiente. Séries são agrupadas pelo título (sem "S01E03"): um item por série.
     * Se hideAdult for verdadeiro, itens adultos (pelo nome ou pela categoria) são pulados.
     */
    public static Sample sample(String m3uUrl, int limit, boolean hideAdult) throws IOException {
        final Sample out = new Sample();
        try {
            readList(m3uUrl, reader -> scan(reader, out, limit, hideAdult));
        } catch (IOException e) {
            // Lista enorme ou conexão que caiu no meio: se já achamos algo, mostra o que deu.
            if (out.live.isEmpty() && out.movie.isEmpty() && out.series.isEmpty()) {
                throw e;
            }
        }
        return out;
    }

    /**
     * Devolve os nomes das categorias (group-title) que têm itens do tipo pedido, na ordem
     * em que aparecem na lista. Lê a lista até o fim (ou até 200 MB ou 1 minuto) e guarda
     * só os nomes das categorias, nunca os itens.
     */
    public static List<String> categories(String m3uUrl, XtreamApi.Kind kind)
            throws IOException {
        final List<String> out = new ArrayList<>();
        final HashSet<String> seen = new HashSet<>();
        try {
            readList(m3uUrl, reader -> walk(reader, (url, grp, name, icon, tvgId, counter) -> {
                if (classify(url, grp, name) == kind && seen.add(grp)) {
                    out.add(cut(grp));
                }
                return out.size() < MAX_CATEGORIES;
            }));
        } catch (IOException e) {
            if (out.isEmpty()) {
                throw e;
            }
        }
        return out;
    }

    /**
     * Devolve uma página de itens do tipo pedido. Se "group" for null, vale qualquer categoria.
     * Pula os primeiros "offset" itens (que já foram mostrados) e devolve até "limit".
     * Se skipAdult for verdadeiro, itens adultos (pelo nome ou pela categoria) são pulados
     * e NÃO contam no "offset". Séries vêm agrupadas pelo título, um item por série.
     * A lista é lida desde o começo a cada chamada, mas só uma página fica na memória.
     */
    public static List<XtreamApi.Item> page(String m3uUrl, XtreamApi.Kind kind, String group,
                                            int offset, int limit, boolean skipAdult)
            throws IOException {
        final List<XtreamApi.Item> out = new ArrayList<>();
        final HashSet<String> seenSeries = new HashSet<>();
        final int[] skipped = {0};
        try {
            readList(m3uUrl, reader -> walk(reader, (url, grp, name, icon, tvgId, counter) -> {
                if (classify(url, grp, name) != kind) {
                    return true;
                }
                if (group != null && !group.equals(grp)) {
                    return true;
                }
                if (skipAdult && (ParentalStore.isAdultCategory(grp)
                        || ParentalStore.isAdultName(name))) {
                    return true;
                }
                String shownName = name;
                if (kind == XtreamApi.Kind.SERIES) {
                    shownName = seriesTitle(name);
                    if (!seenSeries.add(shownName.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                }
                if (skipped[0] < offset) {
                    skipped[0]++;
                    return true;
                }
                if (kind == XtreamApi.Kind.SERIES) {
                    // Série: sem endereço próprio. Os episódios são buscados quando a série abrir.
                    out.add(new XtreamApi.Item(kind, String.valueOf(counter), cut(shownName),
                            icon, grp, "", ""));
                } else {
                    out.add(new XtreamApi.Item(kind, String.valueOf(counter), cut(shownName),
                            icon, grp, tvgId, "", url));
                }
                return out.size() < limit;
            }));
        } catch (IOException e) {
            if (out.isEmpty()) {
                throw e;
            }
        }
        return out;
    }

    /**
     * Devolve os episódios de UMA série, achados lendo a lista e juntando os itens de série
     * cujo título (sem "S01E03") é igual a "seriesName". No máximo 500 episódios, na ordem em
     * que aparecem na lista. Só os episódios dessa série ficam na memória.
     * A tela da série só abre a partir de itens que já passaram pelo controle parental,
     * por isso aqui não se repete essa conferência.
     */
    public static List<Episode> episodes(String m3uUrl, String seriesName) throws IOException {
        final List<Episode> out = new ArrayList<>();
        final String wanted = seriesName == null ? ""
                : cut(seriesName.trim()).toLowerCase(Locale.ROOT);
        if (wanted.isEmpty()) {
            return out;
        }
        try {
            readList(m3uUrl, reader -> walk(reader, (url, grp, name, icon, tvgId, counter) -> {
                if (classify(url, grp, name) != XtreamApi.Kind.SERIES) {
                    return true;
                }
                String title = cut(seriesTitle(name)).toLowerCase(Locale.ROOT);
                if (!title.equals(wanted)) {
                    return true;
                }
                int season = 1;
                int number = 0;
                Matcher m = SEASON_EP.matcher(name);
                if (m.find()) {
                    String s = m.group(1) != null ? m.group(1) : m.group(3);
                    String e = m.group(2) != null ? m.group(2) : m.group(4);
                    season = parseIntSafe(s, 1);
                    number = parseIntSafe(e, 0);
                }
                out.add(new Episode(cut(name), season, number, url));
                return out.size() < MAX_EPISODES;
            }));
        } catch (IOException e) {
            if (out.isEmpty()) {
                throw e;
            }
        }
        return out;
    }

    /**
     * Busca por nome: lê a lista linha por linha e guarda só os itens do tipo pedido cujo
     * nome tem todas as palavras de "query" (sem ligar para maiúsculas nem acentos).
     * Para ao juntar "limit" itens. Séries são comparadas e mostradas pelo título (sem
     * "S01E03"), um item por série. Se skipAdult for verdadeiro, itens adultos (pelo nome
     * ou pela categoria) são pulados. A lista inteira nunca fica na memória.
     * (Agora usa searchStream por dentro; o resultado é o mesmo de antes.)
     */
    public static List<XtreamApi.Item> search(String m3uUrl, XtreamApi.Kind kind, String query,
                                              boolean skipAdult, int limit)
            throws IOException {
        final List<XtreamApi.Item> out = new ArrayList<>();
        searchStream(m3uUrl, kind, query, skipAdult, limit, item -> {
            out.add(item);
            return true;
        });
        return out;
    }

    /**
     * M11b: igual à busca acima, mas AVISA cada resultado assim que o acha (pelo listener),
     * sem esperar a lista inteira. Para ao juntar "limit" itens ou quando o listener devolver
     * false. Se a leitura falhar no meio depois de achar algo, termina sem erro (como a busca
     * antiga). Roda na thread de quem chamou: NUNCA chamar na thread principal.
     *
     * Primeiro confere se o nome completo tem as palavras (conta barata); só os que passam
     * são classificados (conta pesada). O título da série é um pedaço do nome completo.
     */
    public static void searchStream(String m3uUrl, XtreamApi.Kind kind, String query,
                                    boolean skipAdult, int limit,
                                    XtreamApi.ResultListener listener) throws IOException {
        final String[] words = XtreamApi.searchWords(query);
        if (words.length == 0) {
            return;
        }
        final HashSet<String> seenSeries = new HashSet<>();
        final int[] found = {0};
        try {
            readList(m3uUrl, reader -> walk(reader, (url, grp, name, icon, tvgId, counter) -> {
                // Filtro rápido: se o nome completo não tem as palavras, nenhum tipo serve.
                if (!XtreamApi.matchesWords(name, words)) {
                    return true;
                }
                if (classify(url, grp, name) != kind) {
                    return true;
                }
                String shownName = name;
                if (kind == XtreamApi.Kind.SERIES) {
                    shownName = seriesTitle(name);
                    if (!XtreamApi.matchesWords(shownName, words)) {
                        return true;
                    }
                }
                if (skipAdult && (ParentalStore.isAdultCategory(grp)
                        || ParentalStore.isAdultName(name))) {
                    return true;
                }
                XtreamApi.Item item;
                if (kind == XtreamApi.Kind.SERIES) {
                    if (!seenSeries.add(shownName.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                    // Série: sem endereço próprio. Os episódios são buscados quando a série abrir.
                    item = new XtreamApi.Item(kind, String.valueOf(counter), cut(shownName),
                            icon, grp, "", "");
                } else {
                    item = new XtreamApi.Item(kind, String.valueOf(counter), cut(shownName),
                            icon, grp, tvgId, "", url);
                }
                found[0]++;
                if (!listener.onItem(item)) {
                    return false;
                }
                return found[0] < limit;
            }));
        } catch (IOException e) {
            if (found[0] == 0) {
                throw e;
            }
        }
    }

    // ---------- Leitura da lista ----------

    /** Abre a lista, entrega a leitura à tarefa e fecha tudo no fim. */
    private static void readList(String m3uUrl, Job job) throws IOException {
        if (m3uUrl == null || !isHttp(m3uUrl.trim())) {
            throw new IOException("endereco");
        }
        HttpURLConnection conn = open(m3uUrl.trim());
        try {
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IOException("resposta " + code);
            }
            Reader reader = new InputStreamReader(
                    new LimitedStream(conn.getInputStream(), MAX_BYTES), StandardCharsets.UTF_8);
            try {
                job.run(reader);
            } finally {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    // nada a fazer
                }
            }
        } finally {
            conn.disconnect();
        }
    }

    /** Página inicial: separa os itens por tipo e para quando cada tipo chega no limite. */
    private static void scan(Reader reader, final Sample out, final int limit,
                             final boolean hideAdult) throws IOException {
        final HashSet<String> seenSeries = new HashSet<>();
        walk(reader, (url, group, name, icon, tvgId, counter) -> {
            XtreamApi.Kind kind = classify(url, group, name);
            List<XtreamApi.Item> target;
            if (kind == XtreamApi.Kind.LIVE) {
                target = out.live;
            } else if (kind == XtreamApi.Kind.MOVIE) {
                target = out.movie;
            } else {
                target = out.series;
            }
            if (target.size() >= limit) {
                return true;
            }
            if (hideAdult && (ParentalStore.isAdultCategory(group)
                    || ParentalStore.isAdultName(name))) {
                return true;
            }

            if (kind == XtreamApi.Kind.SERIES) {
                String seriesName = seriesTitle(name);
                if (!seenSeries.add(seriesName.toLowerCase(Locale.ROOT))) {
                    return true;
                }
                // Série: sem endereço próprio. Os episódios são buscados na lista quando a série abrir.
                target.add(new XtreamApi.Item(kind, String.valueOf(counter), cut(seriesName),
                        icon, group, "", ""));
            } else {
                target.add(new XtreamApi.Item(kind, String.valueOf(counter), cut(name),
                        icon, group, tvgId, "", url));
            }

            // Continua lendo enquanto algum tipo ainda não chegou no limite
            return !(out.live.size() >= limit && out.movie.size() >= limit
                    && out.series.size() >= limit);
        });
    }

    /**
     * Lê a lista linha por linha e entrega cada item ao "visitor". Para quando o visitor
     * devolver false, quando a lista acabar ou quando passar de 1 minuto.
     */
    private static void walk(Reader reader, Visitor visitor) throws IOException {
        long deadline = System.nanoTime() + MAX_SECONDS * 1000000000L;
        Lines lines = new Lines(reader);
        StringBuilder sb = new StringBuilder();

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

            String icon = isHttp(logo) ? logo : "";
            if (!visitor.entry(line, group, name, icon, tvgId, counter)) {
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

    private static int parseIntSafe(String s, int fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
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

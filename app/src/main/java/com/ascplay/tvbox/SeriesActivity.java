package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tela da série: no alto a capa, o título, a linha de informações e a sinopse; embaixo, à
 * esquerda as temporadas e à direita os episódios da temporada escolhida.
 * Recebe o número da série e o nome (HomeActivity.EXTRA_ID e EXTRA_NAME).
 * Pede ao servidor só os episódios desta série (no máximo 500) e, em outro pedido, a capa e a
 * sinopse (XtreamApi.details, uma vez). Se a capa e a sinopse falharem, a tela abre sem elas.
 *
 * Ao escolher um episódio, abre o player (MainActivity) levando a lista de TODOS os episódios
 * da série, em ordem (temporadas normais primeiro, "Especiais" no fim), para o player tocar
 * o próximo sozinho no fim, inclusive passando para a temporada seguinte. Começa do zero.
 * Também manda a capa (MainActivity.EXTRA_ICON) e o nome "Série - ep. N" para a lista
 * "Continue assistindo".
 *
 * Lembra onde a pessoa parou (SeriesProgress): ao abrir a série, vai para a temporada certa
 * e deixa o último episódio selecionado.
 *
 * Botão "Continuar de onde parei" (escondido se não há nada para continuar): vai direto ao
 * episódio onde a pessoa parou, no minuto exato (PlaybackProgress). Se esse episódio já
 * terminou, leva ao próximo, do começo. O foco abre nele.
 *
 * Cores: o título do episódio visto fica verde; no visto pela metade só a parte
 * correspondente do título fica verde. As cores e o botão são refeitos ao voltar do player.
 *
 * Esta tela só é aberta a partir de itens que já passaram pelo controle parental
 * (página inicial e "Ver mais"), por isso não repete a conferência da senha.
 */
public class SeriesActivity extends Activity {

    /** Lista de números (ids) dos episódios da série, na ordem de reprodução. */
    public static final String EXTRA_LIST_IDS = "list_ids";
    /** Lista das extensões (mp4, mkv...) na mesma ordem da lista de ids. */
    public static final String EXTRA_LIST_EXTS = "list_exts";
    /** Posição do episódio escolhido dentro da lista. */
    public static final String EXTRA_LIST_INDEX = "list_index";

    private static final int COLOR_NEUTRAL = 0xFFA39A9D;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    private static final int COLOR_PLOT = 0xFFE6DFE1;
    // Cor do título do episódio já visto (ou da parte vista)
    private static final int COLOR_SEEN = 0xFF4ADE80;

    private CredentialStore.Credentials creds;
    private String seriesName = "";
    private String seriesKey = "";
    // Capa da série (a do item; trocada pela do servidor, se ela chegar)
    private String seriesIcon = "";

    private TextView tvTitle;
    private TextView tvStatus;
    private TextView tvMeta;
    private TextView tvPlot;
    private ImageView ivCover;
    private Button btnContinue;
    private ListView lvSeasons;
    private ListView lvEpisodes;

    // Todos os episódios (já em ordem), as temporadas que existem e os episódios mostrados agora
    private final List<XtreamApi.Episode> allEpisodes = new ArrayList<>();
    private final List<Integer> seasons = new ArrayList<>();
    private final List<XtreamApi.Episode> shown = new ArrayList<>();

    private SeasonAdapter seasonAdapter;
    private EpisodeAdapter episodeAdapter;
    private int currentSeason = -1;

    // Episódio e minuto do botão "Continuar de onde parei" (null = nada para continuar)
    private XtreamApi.Episode continueEpisode = null;
    private long continueMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        creds = CredentialStore.load(this);
        String id = getIntent().getStringExtra(HomeActivity.EXTRA_ID);
        String name = getIntent().getStringExtra(HomeActivity.EXTRA_NAME);
        String icon = getIntent().getStringExtra(MovieActivity.EXTRA_ICON);
        if (creds == null || !CredentialStore.MODE_XTREAM.equals(creds.mode)
                || id == null || !id.matches("\\d{1,12}")) {
            finish();
            return;
        }
        seriesName = name == null ? "" : name;
        if (icon != null) {
            seriesIcon = icon;
        }
        // Chave da série para lembrar onde parou (o player só guarda um resumo embaralhado dela)
        seriesKey = "xt|" + creds.server + "|" + creds.user + "|" + id;

        setContentView(R.layout.activity_series);
        tvTitle = findViewById(R.id.tv_title);
        tvStatus = findViewById(R.id.tv_status);
        tvMeta = findViewById(R.id.tv_meta);
        tvPlot = findViewById(R.id.tv_plot);
        ivCover = findViewById(R.id.iv_cover);
        btnContinue = findViewById(R.id.btn_continue);
        lvSeasons = findViewById(R.id.lv_seasons);
        lvEpisodes = findViewById(R.id.lv_episodes);

        tvTitle.setText(seriesName);
        tvPlot.setTextColor(COLOR_NEUTRAL);
        tvPlot.setText("Carregando...");
        if (icon != null && !icon.isEmpty()) {
            ImageLoader.load(ivCover, icon);
        }
        lvSeasons.setDividerHeight(dp(6));
        lvEpisodes.setDividerHeight(dp(6));

        seasonAdapter = new SeasonAdapter();
        episodeAdapter = new EpisodeAdapter();
        lvSeasons.setAdapter(seasonAdapter);
        lvEpisodes.setAdapter(episodeAdapter);

        // Mexer nas temporadas com as setas já troca a lista de episódios
        lvSeasons.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long rowId) {
                showSeason(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nada a fazer
            }
        });
        // OK numa temporada leva o foco para os episódios
        lvSeasons.setOnItemClickListener((parent, view, position, rowId) -> {
            showSeason(position);
            if (!shown.isEmpty()) {
                lvEpisodes.requestFocus();
            }
        });
        lvEpisodes.setOnItemClickListener((parent, view, position, rowId) -> {
            if (position >= 0 && position < shown.size()) {
                playEpisode(shown.get(position), 0L);
            }
        });
        btnContinue.setOnClickListener(v -> {
            if (continueEpisode != null) {
                playEpisode(continueEpisode, continueMs);
            }
        });

        loadEpisodes(id);
        loadDetails(id);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Voltou do player: atualiza o botão "Continuar" e as cores dos episódios
        if (!allEpisodes.isEmpty()) {
            refreshContinue();
            episodeAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clear();
    }

    // ---------- Capa e sinopse ----------

    /** Pede capa e sinopse ao servidor (uma série só). Se falhar, a tela fica sem elas. */
    private void loadDetails(final String seriesId) {
        final String server = creds.server;
        final String user = creds.user;
        final String pass = creds.pass;
        new Thread(() -> {
            XtreamApi.Details details = null;
            try {
                details = XtreamApi.details(server, user, pass, XtreamApi.Kind.SERIES, seriesId);
            } catch (Exception e) {
                // sem rede ou resposta estranha: a tela fica sem sinopse
            }
            final XtreamApi.Details result = details;
            runOnUiThread(() -> showDetails(result));
        }).start();
    }

    private void showDetails(XtreamApi.Details d) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (d == null) {
            tvPlot.setText("Sem sinopse para esta série.");
            return;
        }
        if (!d.cover.isEmpty()) {
            seriesIcon = d.cover;
            ImageLoader.load(ivCover, d.cover);
        }
        if (!d.meta.isEmpty()) {
            tvMeta.setText(d.meta);
            tvMeta.setVisibility(View.VISIBLE);
        }
        if (d.plot.isEmpty()) {
            tvPlot.setText("Sem sinopse para esta série.");
        } else {
            tvPlot.setTextColor(COLOR_PLOT);
            tvPlot.setText(d.plot);
        }
    }

    // ---------- Carregar os episódios ----------

    private void loadEpisodes(final String seriesId) {
        final String server = creds.server;
        final String user = creds.user;
        final String pass = creds.pass;
        new Thread(() -> {
            List<XtreamApi.Episode> result = null;
            try {
                result = XtreamApi.episodes(server, user, pass, seriesId);
            } catch (Exception e) {
                // sem rede, servidor fora do ar ou resposta estranha: mostra aviso abaixo
            }
            final List<XtreamApi.Episode> res = result;
            runOnUiThread(() -> onEpisodes(res));
        }).start();
    }

    private void onEpisodes(List<XtreamApi.Episode> res) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (res == null) {
            showStatus("Não foi possível carregar. Confira a internet e o servidor.", COLOR_ERROR);
            return;
        }
        if (res.isEmpty()) {
            showStatus("Esta série não tem episódios.", COLOR_NEUTRAL);
            return;
        }

        allEpisodes.clear();
        allEpisodes.addAll(res);
        Collections.sort(allEpisodes, new Comparator<XtreamApi.Episode>() {
            @Override
            public int compare(XtreamApi.Episode a, XtreamApi.Episode b) {
                if (a.season != b.season) {
                    return a.season < b.season ? -1 : 1;
                }
                if (a.number != b.number) {
                    return a.number < b.number ? -1 : 1;
                }
                return 0;
            }
        });

        seasons.clear();
        int last = Integer.MIN_VALUE;
        for (XtreamApi.Episode e : allEpisodes) {
            if (e.season != last) {
                seasons.add(e.season);
                last = e.season;
            }
        }
        seasonAdapter.notifyDataSetChanged();

        tvStatus.setVisibility(View.GONE);
        currentSeason = -1;

        // Botão "Continuar de onde parei" (aparece só se já assistiu alguma coisa)
        boolean hasContinue = refreshContinue();

        // Se a pessoa já assistiu esta série, abre no episódio onde parou
        XtreamApi.Episode resume = null;
        int saved = SeriesProgress.load(this, seriesKey);
        if (saved >= 0) {
            List<XtreamApi.Episode> queue = buildQueue();
            if (saved < queue.size()) {
                resume = queue.get(saved);
            }
        }
        if (resume != null) {
            int seasonPos = seasons.indexOf(resume.season);
            if (seasonPos < 0) {
                seasonPos = 0;
            }
            showSeason(seasonPos);
            lvSeasons.setSelection(seasonPos);
            int epPos = shown.indexOf(resume);
            if (epPos >= 0) {
                lvEpisodes.setSelection(epPos);
            }
            if (hasContinue) {
                btnContinue.requestFocus();
            } else {
                lvEpisodes.requestFocus();
            }
            return;
        }

        showSeason(0);
        if (seasons.size() == 1) {
            lvEpisodes.requestFocus();
        } else {
            lvSeasons.requestFocus();
        }
    }

    // ---------- Continuar de onde parei ----------

    /**
     * Confere o que dá para continuar e mostra ou esconde o botão. Devolve true se o botão
     * ficou visível. Não mexe no foco.
     */
    private boolean refreshContinue() {
        continueEpisode = null;
        continueMs = 0L;

        if (!allEpisodes.isEmpty()) {
            int saved = SeriesProgress.load(this, seriesKey);
            List<XtreamApi.Episode> queue = buildQueue();
            if (saved >= 0 && saved < queue.size()) {
                XtreamApi.Episode ep = queue.get(saved);
                PlaybackProgress.Mark mark = markOf(ep);
                if (mark != null && mark.isFinished()) {
                    // Já terminou este episódio: o "Continuar" leva ao próximo, do começo
                    if (saved + 1 < queue.size()) {
                        continueEpisode = queue.get(saved + 1);
                    }
                } else {
                    continueEpisode = ep;
                    if (mark != null && mark.isPartial()) {
                        continueMs = mark.positionMs;
                    }
                }
            }
        }

        if (continueEpisode == null) {
            btnContinue.setVisibility(View.GONE);
            return false;
        }
        String text = "Continuar de onde parei (" + shortLabel(continueEpisode);
        if (continueMs > 0L) {
            text += ", " + clock(continueMs);
        }
        text += ")";
        btnContinue.setText(text);
        btnContinue.setVisibility(View.VISIBLE);
        return true;
    }

    /** O que foi anotado para este episódio (null se nada). */
    private PlaybackProgress.Mark markOf(XtreamApi.Episode ep) {
        return PlaybackProgress.load(this,
                PlaybackProgress.episodeKey(creds.server, creds.user, ep.id));
    }

    /** Texto curto do episódio: "T2 E3" (ou "Especiais E3"). */
    private static String shortLabel(XtreamApi.Episode e) {
        String s = e.season == 0 ? "Especiais" : "T" + e.season;
        if (e.number > 0) {
            s += " E" + e.number;
        }
        return s;
    }

    /** Minuto em texto: 5:07 ou 1:23:45. */
    private static String clock(long ms) {
        long total = ms / 1000L;
        long h = total / 3600L;
        long m = (total % 3600L) / 60L;
        long s = total % 60L;
        if (h > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    // ---------- Temporadas e episódios ----------

    private void showSeason(int position) {
        if (position < 0 || position >= seasons.size() || position == currentSeason) {
            return;
        }
        currentSeason = position;
        int season = seasons.get(position);
        shown.clear();
        for (XtreamApi.Episode e : allEpisodes) {
            if (e.season == season) {
                shown.add(e);
            }
        }
        episodeAdapter.notifyDataSetChanged();
        lvEpisodes.setSelection(0);

        if (seasons.size() > 1) {
            tvTitle.setText(seriesName + " - " + seasonLabel(season));
        } else {
            tvTitle.setText(seriesName);
        }
    }

    /** Fila de reprodução: temporadas normais em ordem, e os "Especiais" (temporada 0) no fim. */
    private ArrayList<XtreamApi.Episode> buildQueue() {
        ArrayList<XtreamApi.Episode> queue = new ArrayList<>();
        for (XtreamApi.Episode e : allEpisodes) {
            if (e.season != 0) {
                queue.add(e);
            }
        }
        for (XtreamApi.Episode e : allEpisodes) {
            if (e.season == 0) {
                queue.add(e);
            }
        }
        return queue;
    }

    /** Abre o player neste episódio. Se startMs for maior que zero, começa naquele minuto. */
    private void playEpisode(XtreamApi.Episode chosen, long startMs) {
        ArrayList<XtreamApi.Episode> queue = buildQueue();
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<String> exts = new ArrayList<>();
        int index = 0;
        for (int n = 0; n < queue.size(); n++) {
            XtreamApi.Episode e = queue.get(n);
            ids.add(e.id);
            exts.add(e.ext);
            if (e == chosen) {
                index = n;
            }
        }

        // Nome para a lista "Continue assistindo": "Série - ep. N" (igual ao do player)
        String continueName = (seriesName.isEmpty() ? "" : seriesName + " - ") + "ep. " + (index + 1);

        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(HomeActivity.EXTRA_KIND, XtreamApi.Kind.EPISODE.name());
        i.putExtra(HomeActivity.EXTRA_ID, chosen.id);
        i.putExtra(HomeActivity.EXTRA_EXT, chosen.ext);
        i.putExtra(HomeActivity.EXTRA_NAME, continueName);
        i.putStringArrayListExtra(EXTRA_LIST_IDS, ids);
        i.putStringArrayListExtra(EXTRA_LIST_EXTS, exts);
        i.putExtra(EXTRA_LIST_INDEX, index);
        i.putExtra(MainActivity.EXTRA_SERIES_KEY, seriesKey);
        if (seriesIcon != null && !seriesIcon.isEmpty()) {
            i.putExtra(MainActivity.EXTRA_ICON, seriesIcon);
        }
        if (startMs > 0L) {
            i.putExtra(MainActivity.EXTRA_START_MS, startMs);
        }
        startActivity(i);
    }

    // ---------- Textos ----------

    private static String seasonLabel(int season) {
        return season == 0 ? "Especiais" : "Temporada " + season;
    }

    private static String episodeLabel(XtreamApi.Episode e) {
        String title = e.title == null ? "" : e.title;
        if (e.number > 0) {
            return title.isEmpty() ? "Episódio " + e.number : e.number + ". " + title;
        }
        return title.isEmpty() ? "Episódio" : title;
    }

    /**
     * Pinta o título do episódio: visto = tudo verde; visto pela metade = só a parte
     * correspondente do título fica verde; não visto = como era.
     */
    private static CharSequence paint(String label, PlaybackProgress.Mark mark) {
        if (mark == null || label.isEmpty()) {
            return label;
        }
        int count;
        if (mark.isFinished()) {
            count = label.length();
        } else if (mark.isPartial()) {
            count = (int) Math.round(label.length() * mark.fraction());
            if (count < 1) {
                count = 1;
            }
        } else {
            return label;
        }
        SpannableString sp = new SpannableString(label);
        sp.setSpan(new ForegroundColorSpan(COLOR_SEEN), 0, count,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sp;
    }

    private void showStatus(String text, int color) {
        tvStatus.setTextColor(color);
        tvStatus.setText(text);
        tvStatus.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ---------- Adaptadores (reaproveitam as linhas ao rolar) ----------

    private final class SeasonAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return seasons.size();
        }

        @Override
        public Object getItem(int position) {
            return seasons.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView == null) {
                tv = (TextView) LayoutInflater.from(SeriesActivity.this)
                        .inflate(R.layout.item_category, parent, false);
                // Quem controla a seleção e o clique é a própria lista.
                tv.setFocusable(false);
                tv.setClickable(false);
                tv.setMaxLines(1);
                tv.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                tv = (TextView) convertView;
            }
            tv.setText(seasonLabel(seasons.get(position)));
            return tv;
        }
    }

    private final class EpisodeAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return shown.size();
        }

        @Override
        public Object getItem(int position) {
            return shown.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView == null) {
                tv = (TextView) LayoutInflater.from(SeriesActivity.this)
                        .inflate(R.layout.item_category, parent, false);
                tv.setFocusable(false);
                tv.setClickable(false);
                tv.setMaxLines(1);
                tv.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                tv = (TextView) convertView;
            }
            XtreamApi.Episode ep = shown.get(position);
            tv.setText(paint(episodeLabel(ep), markOf(ep)));
            return tv;
        }
    }
}

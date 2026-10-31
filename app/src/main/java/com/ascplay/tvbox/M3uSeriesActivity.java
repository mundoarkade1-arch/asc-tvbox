package com.ascplay.tvbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tela da série para o login por lista M3U: no alto a capa do item e o título (a lista M3U não
 * tem sinopse: aparece "Sem sinopse nesta lista."); abaixo os botões "Continuar" e "Assistir",
 * o botão da temporada (abre uma janelinha com todas as temporadas) e a grade de episódios
 * (15 colunas, só o número de cada episódio). Recebe o título da série
 * (HomeActivity.EXTRA_NAME) e, se houver, o endereço da capa (MovieActivity.EXTRA_ICON).
 *
 * Os episódios são achados lendo a lista aos poucos e juntando os itens de série com o mesmo
 * título (M3uApi.episodes), no máximo 500. Só os episódios desta série ficam na memória.
 *
 * Ao escolher um episódio, abre o player (MainActivity) levando o endereço dele e a lista de
 * endereços de TODOS os episódios da série, em ordem (temporadas normais primeiro, "Especiais"
 * no fim), para o player tocar o próximo sozinho. Se a lista de endereços for grande demais
 * para passar entre telas, vai só o episódio escolhido. Também manda a capa
 * (MainActivity.EXTRA_ICON), o nome "Série - ep. N" para a lista "Continue assistindo" e, em
 * MainActivity.EXTRA_LIST_LABELS, o texto "Temporada N - Episódio N" de cada episódio (M13).
 *
 * Botão "Continuar" (escondido se não há nada para continuar): vai ao episódio onde a pessoa
 * parou, no minuto exato (PlaybackProgress, chave = endereço do vídeo). Se esse episódio já
 * terminou, leva ao próximo, do começo. Botão "Assistir": toca o episódio atual (o mesmo do
 * "Continuar") do começo; se a série nunca foi vista, toca o primeiro episódio.
 *
 * Grade: os quadradinhos NÃO recebem foco; quem controla as setas, a seleção e a rolagem é a
 * própria grade (como nas outras listas do app). O clique é da grade (setOnItemClickListener).
 *
 * Cores dos episódios: visto = vermelho; atual (o do "Continuar") = verde; resto = branco.
 * As cores e os botões são refeitos ao voltar do player.
 *
 * As listas antigas (lv_seasons e lv_episodes) continuam no layout, mas ficam escondidas.
 *
 * Esta tela só é aberta a partir de itens que já passaram pelo controle parental
 * (página inicial e "Ver mais"), por isso não repete a conferência da senha.
 * Os endereços dos vídeos costumam conter usuário e senha: nunca mostrar nem registrar em log.
 */
public class M3uSeriesActivity extends Activity {

    private static final int COLOR_NEUTRAL = 0xFFA99FA3;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    // Cores dos episódios na grade
    private static final int COLOR_WATCHED = 0xFFFF4D5E;
    private static final int COLOR_CURRENT = 0xFF4ADE80;
    private static final int COLOR_TODO = 0xFFFFFFFF;
    // Limite de texto (em letras) dos endereços mandados ao player de uma vez
    private static final int MAX_LIST_CHARS = 100000;

    private CredentialStore.Credentials creds;
    private String seriesName = "";
    private String seriesKey = "";
    // Capa da série (a do item, se veio)
    private String seriesIcon = "";

    private TextView tvTitle;
    private TextView tvStatus;
    private TextView tvPlot;
    private ImageView ivCover;
    private Button btnContinue;
    private Button btnWatch;
    private Button btnSeason;
    private GridView gvEpisodes;
    // Caixa do aviso (a que guarda o tv_status)
    private View statusBox;

    // Todos os episódios (já em ordem), as temporadas que existem e os episódios mostrados agora
    private final List<M3uApi.Episode> allEpisodes = new ArrayList<>();
    private final List<Integer> seasons = new ArrayList<>();
    private final List<M3uApi.Episode> shown = new ArrayList<>();

    private EpisodeAdapter episodeAdapter;
    private int currentSeason = -1;

    // Episódio e minuto do botão "Continuar" (null = nada para continuar)
    private M3uApi.Episode continueEpisode = null;
    private long continueMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        creds = CredentialStore.load(this);
        String name = getIntent().getStringExtra(HomeActivity.EXTRA_NAME);
        String icon = getIntent().getStringExtra(MovieActivity.EXTRA_ICON);
        if (creds == null || !CredentialStore.MODE_M3U.equals(creds.mode)
                || creds.m3uUrl == null || name == null || name.trim().isEmpty()) {
            finish();
            return;
        }
        seriesName = name.trim();
        if (icon != null) {
            seriesIcon = icon;
        }
        // Chave da série para lembrar onde parou (só um resumo embaralhado dela é guardado,
        // porque o endereço da lista pode conter usuário e senha)
        seriesKey = "m3u|" + creds.m3uUrl + "|" + seriesName;

        setContentView(R.layout.activity_series);
        tvTitle = findViewById(R.id.tv_title);
        tvStatus = findViewById(R.id.tv_status);
        tvPlot = findViewById(R.id.tv_plot);
        ivCover = findViewById(R.id.iv_cover);
        btnContinue = findViewById(R.id.btn_continue);
        btnWatch = findViewById(R.id.btn_watch);
        btnSeason = findViewById(R.id.btn_season);
        gvEpisodes = findViewById(R.id.gv_episodes);
        statusBox = (View) tvStatus.getParent().getParent();

        // As listas antigas ficam escondidas (o layout ainda as tem)
        findViewById(R.id.lv_seasons).setVisibility(View.GONE);
        findViewById(R.id.lv_episodes).setVisibility(View.GONE);

        tvTitle.setText(seriesName);
        // A lista M3U não traz sinopse
        tvPlot.setTextColor(COLOR_NEUTRAL);
        tvPlot.setText("Sem sinopse nesta lista.");
        if (icon != null && !icon.isEmpty()) {
            ImageLoader.load(ivCover, icon);
        }

        episodeAdapter = new EpisodeAdapter();
        gvEpisodes.setAdapter(episodeAdapter);
        // O clique é da grade (os quadradinhos não recebem foco)
        gvEpisodes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
                if (position >= 0 && position < shown.size()) {
                    playEpisode(shown.get(position), 0L);
                }
            }
        });

        btnContinue.setText("Continuar");
        btnContinue.setOnClickListener(v -> {
            if (continueEpisode != null) {
                playEpisode(continueEpisode, continueMs);
            }
        });
        btnWatch.setOnClickListener(v -> watchFromStart());
        btnSeason.setOnClickListener(v -> chooseSeason());

        showStatus("Carregando...", COLOR_NEUTRAL);
        loadEpisodes();
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

    // ---------- Carregar os episódios ----------

    private void loadEpisodes() {
        final String url = creds.m3uUrl;
        final String name = seriesName;
        new Thread(() -> {
            List<M3uApi.Episode> result = null;
            try {
                result = M3uApi.episodes(url, name);
            } catch (Exception e) {
                // sem rede, lista fora do ar ou resposta estranha: mostra aviso abaixo
            }
            final List<M3uApi.Episode> res = result;
            runOnUiThread(() -> onEpisodes(res));
        }).start();
    }

    private void onEpisodes(List<M3uApi.Episode> res) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (res == null) {
            showStatus("Não foi possível carregar. Confira a internet e a lista.", COLOR_ERROR);
            return;
        }
        if (res.isEmpty()) {
            showStatus("Não foram achados episódios desta série.", COLOR_NEUTRAL);
            return;
        }

        allEpisodes.clear();
        allEpisodes.addAll(res);
        // A ordenação mantém a ordem da lista quando temporada e número são iguais
        Collections.sort(allEpisodes, new Comparator<M3uApi.Episode>() {
            @Override
            public int compare(M3uApi.Episode a, M3uApi.Episode b) {
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
        for (M3uApi.Episode e : allEpisodes) {
            if (e.season != last) {
                seasons.add(e.season);
                last = e.season;
            }
        }

        // Some o aviso e aparece a grade
        statusBox.setVisibility(View.GONE);
        gvEpisodes.setVisibility(View.VISIBLE);
        btnWatch.setVisibility(View.VISIBLE);
        btnSeason.setVisibility(seasons.size() > 1 ? View.VISIBLE : View.GONE);
        currentSeason = -1;

        // Botão "Continuar" (aparece só se já assistiu alguma coisa)
        boolean hasContinue = refreshContinue();

        // Abre na temporada do episódio atual (ou na primeira)
        int seasonPos = 0;
        M3uApi.Episode focusEp = continueEpisode;
        if (focusEp != null) {
            int p = seasons.indexOf(focusEp.season);
            if (p >= 0) {
                seasonPos = p;
            }
        }
        showSeason(seasonPos);
        if (focusEp != null) {
            int epPos = shown.indexOf(focusEp);
            if (epPos >= 0) {
                gvEpisodes.setSelection(epPos);
            }
        }

        if (hasContinue) {
            btnContinue.requestFocus();
        } else {
            btnWatch.requestFocus();
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
            List<M3uApi.Episode> queue = buildQueue();
            if (saved >= 0 && saved < queue.size()) {
                M3uApi.Episode ep = queue.get(saved);
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
        btnContinue.setVisibility(View.VISIBLE);
        return true;
    }

    /** "Assistir": toca o episódio atual do começo (ou o primeiro, se nunca viu a série). */
    private void watchFromStart() {
        M3uApi.Episode target = continueEpisode;
        if (target == null) {
            List<M3uApi.Episode> queue = buildQueue();
            if (queue.isEmpty()) {
                return;
            }
            target = queue.get(0);
        }
        playEpisode(target, 0L);
    }

    /** O que foi anotado para este episódio (null se nada). A chave é o endereço do vídeo. */
    private PlaybackProgress.Mark markOf(M3uApi.Episode ep) {
        if (ep == null || ep.url == null || ep.url.isEmpty()) {
            return null;
        }
        return PlaybackProgress.load(this, PlaybackProgress.urlKey(ep.url));
    }

    // ---------- Temporadas e episódios ----------

    /** Abre a janelinha com todas as temporadas. */
    private void chooseSeason() {
        if (seasons.isEmpty()) {
            return;
        }
        CharSequence[] labels = new CharSequence[seasons.size()];
        for (int n = 0; n < seasons.size(); n++) {
            labels[n] = seasonLabel(seasons.get(n));
        }
        new AlertDialog.Builder(this)
                .setTitle("Escolha a temporada")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showSeason(which);
                        if (!shown.isEmpty()) {
                            gvEpisodes.requestFocus();
                        }
                    }
                })
                .show();
    }

    private void showSeason(int position) {
        if (position < 0 || position >= seasons.size() || position == currentSeason) {
            return;
        }
        currentSeason = position;
        int season = seasons.get(position);
        shown.clear();
        for (M3uApi.Episode e : allEpisodes) {
            if (e.season == season) {
                shown.add(e);
            }
        }
        episodeAdapter.notifyDataSetChanged();
        gvEpisodes.setSelection(0);
        btnSeason.setText(seasonLabel(season));
    }

    /** Fila de reprodução: temporadas normais em ordem, e os "Especiais" (temporada 0) no fim. */
    private ArrayList<M3uApi.Episode> buildQueue() {
        ArrayList<M3uApi.Episode> queue = new ArrayList<>();
        for (M3uApi.Episode e : allEpisodes) {
            if (e.season != 0) {
                queue.add(e);
            }
        }
        for (M3uApi.Episode e : allEpisodes) {
            if (e.season == 0) {
                queue.add(e);
            }
        }
        return queue;
    }

    /**
     * M13: texto "Temporada N - Episódio N" de cada episódio, na ordem da fila.
     * O número é o da lista; sem número, é a posição do episódio dentro da temporada
     * (igual ao que a grade mostra).
     */
    private ArrayList<String> buildLabels(List<M3uApi.Episode> queue) {
        Map<M3uApi.Episode, String> byEpisode = new HashMap<>();
        Map<Integer, Integer> counters = new HashMap<>();
        for (M3uApi.Episode e : allEpisodes) {
            Integer c = counters.get(e.season);
            int pos = c == null ? 0 : c;
            counters.put(e.season, pos + 1);
            byEpisode.put(e, seasonLabel(e.season) + " - Episódio " + numberLabel(e, pos));
        }
        ArrayList<String> labels = new ArrayList<>();
        for (M3uApi.Episode e : queue) {
            String t = byEpisode.get(e);
            labels.add(t == null ? "" : t);
        }
        return labels;
    }

    /** Abre o player neste episódio. Se startMs for maior que zero, começa naquele minuto. */
    private void playEpisode(M3uApi.Episode chosen, long startMs) {
        ArrayList<M3uApi.Episode> queue = buildQueue();
        int index = 0;
        for (int n = 0; n < queue.size(); n++) {
            if (queue.get(n) == chosen) {
                index = n;
                break;
            }
        }

        // Nome para a lista "Continue assistindo": "Série - ep. N" (igual ao do player)
        String continueName = (seriesName.isEmpty() ? "" : seriesName + " - ") + "ep. " + (index + 1);

        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(HomeActivity.EXTRA_KIND, XtreamApi.Kind.EPISODE.name());
        // O player pede um número de identificação: usamos a posição do episódio na fila
        i.putExtra(HomeActivity.EXTRA_ID, String.valueOf(index + 1));
        i.putExtra(HomeActivity.EXTRA_EXT, "");
        i.putExtra(HomeActivity.EXTRA_NAME, continueName);
        i.putExtra(MainActivity.EXTRA_URL, chosen.url);
        i.putExtra(MainActivity.EXTRA_SERIES_KEY, seriesKey);
        if (seriesIcon != null && !seriesIcon.isEmpty()) {
            i.putExtra(MainActivity.EXTRA_ICON, seriesIcon);
        }
        if (startMs > 0L) {
            i.putExtra(MainActivity.EXTRA_START_MS, startMs);
        }

        // Manda a lista dos endereços da série, se couber (para tocar o próximo sozinho)
        long chars = 0;
        for (M3uApi.Episode e : queue) {
            chars += e.url.length();
        }
        if (chars <= MAX_LIST_CHARS) {
            ArrayList<String> urls = new ArrayList<>();
            for (M3uApi.Episode e : queue) {
                urls.add(e.url);
            }
            i.putStringArrayListExtra(MainActivity.EXTRA_LIST_URLS, urls);
            i.putStringArrayListExtra(MainActivity.EXTRA_LIST_LABELS, buildLabels(queue));
            i.putExtra(SeriesActivity.EXTRA_LIST_INDEX, index);
        }
        startActivity(i);
    }

    // ---------- Textos e cores ----------

    private static String seasonLabel(int season) {
        return season == 0 ? "Especiais" : "Temporada " + season;
    }

    /** Só o número do episódio. Sem número, usa a posição dele na temporada. */
    private static String numberLabel(M3uApi.Episode e, int position) {
        return String.valueOf(e.number > 0 ? e.number : position + 1);
    }

    /** Cor do episódio: atual = verde; visto = vermelho; resto = branco. */
    private int colorOf(M3uApi.Episode ep) {
        if (ep == continueEpisode) {
            return COLOR_CURRENT;
        }
        PlaybackProgress.Mark mark = markOf(ep);
        if (mark != null && mark.isFinished()) {
            return COLOR_WATCHED;
        }
        return COLOR_TODO;
    }

    private void showStatus(String text, int color) {
        tvStatus.setTextColor(color);
        tvStatus.setText(text);
        tvStatus.setVisibility(View.VISIBLE);
        statusBox.setVisibility(View.VISIBLE);
        gvEpisodes.setVisibility(View.GONE);
    }

    // ---------- Adaptador da grade (reaproveita os quadradinhos ao rolar) ----------

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
                tv = (TextView) LayoutInflater.from(M3uSeriesActivity.this)
                        .inflate(R.layout.item_episode, parent, false);
                // Quem controla a seleção e o clique é a própria grade.
                tv.setFocusable(false);
                tv.setClickable(false);
            } else {
                tv = (TextView) convertView;
            }
            M3uApi.Episode ep = shown.get(position);
            tv.setText(numberLabel(ep, position));
            tv.setTextColor(colorOf(ep));
            return tv;
        }
    }
}

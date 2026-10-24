package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tela da série para o login por lista M3U: à esquerda as temporadas, à direita os episódios
 * da temporada escolhida. Recebe o título da série (HomeActivity.EXTRA_NAME).
 *
 * Os episódios são achados lendo a lista aos poucos e juntando os itens de série com o mesmo
 * título (M3uApi.episodes), no máximo 500. Só os episódios desta série ficam na memória.
 *
 * Ao escolher um episódio, abre o player (MainActivity) levando o endereço dele e a lista de
 * endereços de TODOS os episódios da série, em ordem (temporadas normais primeiro, "Especiais"
 * no fim), para o player tocar o próximo sozinho, inclusive passando para a temporada seguinte.
 * Se a lista de endereços for grande demais para passar entre telas, vai só o episódio escolhido.
 *
 * Lembra onde a pessoa parou (SeriesProgress): ao abrir a série, vai para a temporada certa
 * e deixa o último episódio selecionado.
 *
 * Esta tela só é aberta a partir de itens que já passaram pelo controle parental
 * (página inicial e "Ver mais"), por isso não repete a conferência da senha.
 * Os endereços dos vídeos costumam conter usuário e senha: nunca mostrar nem registrar em log.
 */
public class M3uSeriesActivity extends Activity {

    private static final int COLOR_NEUTRAL = 0xFFA39A9D;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    // Limite de texto (em letras) dos endereços mandados ao player de uma vez
    private static final int MAX_LIST_CHARS = 100000;

    private CredentialStore.Credentials creds;
    private String seriesName = "";
    private String seriesKey = "";

    private TextView tvTitle;
    private TextView tvStatus;
    private ListView lvSeasons;
    private ListView lvEpisodes;

    // Todos os episódios (já em ordem), as temporadas que existem e os episódios mostrados agora
    private final List<M3uApi.Episode> allEpisodes = new ArrayList<>();
    private final List<Integer> seasons = new ArrayList<>();
    private final List<M3uApi.Episode> shown = new ArrayList<>();

    private SeasonAdapter seasonAdapter;
    private EpisodeAdapter episodeAdapter;
    private int currentSeason = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        creds = CredentialStore.load(this);
        String name = getIntent().getStringExtra(HomeActivity.EXTRA_NAME);
        if (creds == null || !CredentialStore.MODE_M3U.equals(creds.mode)
                || creds.m3uUrl == null || name == null || name.trim().isEmpty()) {
            finish();
            return;
        }
        seriesName = name.trim();
        // Chave da série para lembrar onde parou (só um resumo embaralhado dela é guardado,
        // porque o endereço da lista pode conter usuário e senha)
        seriesKey = "m3u|" + creds.m3uUrl + "|" + seriesName;

        setContentView(R.layout.activity_series);
        tvTitle = findViewById(R.id.tv_title);
        tvStatus = findViewById(R.id.tv_status);
        lvSeasons = findViewById(R.id.lv_seasons);
        lvEpisodes = findViewById(R.id.lv_episodes);

        tvTitle.setText(seriesName);
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
        lvEpisodes.setOnItemClickListener((parent, view, position, rowId) -> playEpisode(position));

        showStatus("Carregando...", COLOR_NEUTRAL);
        loadEpisodes();
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
        seasonAdapter.notifyDataSetChanged();

        tvStatus.setVisibility(View.GONE);
        currentSeason = -1;

        // Se a pessoa já assistiu esta série, abre no episódio onde parou
        M3uApi.Episode resume = null;
        int saved = SeriesProgress.load(this, seriesKey);
        if (saved >= 0) {
            List<M3uApi.Episode> queue = buildQueue();
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
            lvEpisodes.requestFocus();
            return;
        }

        showSeason(0);
        if (seasons.size() == 1) {
            lvEpisodes.requestFocus();
        } else {
            lvSeasons.requestFocus();
        }
    }

    // ---------- Temporadas e episódios ----------

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
 * Tela da série: à esquerda as temporadas, à direita os episódios da temporada escolhida.
 * Recebe o número da série e o nome (HomeActivity.EXTRA_ID e EXTRA_NAME).
 * Pede ao servidor só os episódios desta série (no máximo 500).
 *
 * Ao escolher um episódio, abre o player (MainActivity) levando a lista de TODOS os episódios
 * da série, em ordem (temporadas normais primeiro, "Especiais" no fim), para o player tocar
 * o próximo sozinho no fim, inclusive passando para a temporada seguinte.
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

    private CredentialStore.Credentials creds;
    private String seriesName = "";

    private TextView tvTitle;
    private TextView tvStatus;
    private ListView lvSeasons;
    private ListView lvEpisodes;

    // Todos os episódios (já em ordem), as temporadas que existem e os episódios mostrados agora
    private final List<XtreamApi.Episode> allEpisodes = new ArrayList<>();
    private final List<Integer> seasons = new ArrayList<>();
    private final List<XtreamApi.Episode> shown = new ArrayList<>();

    private SeasonAdapter seasonAdapter;
    private EpisodeAdapter episodeAdapter;
    private int currentSeason = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        creds = CredentialStore.load(this);
        String id = getIntent().getStringExtra(HomeActivity.EXTRA_ID);
        String name = getIntent().getStringExtra(HomeActivity.EXTRA_NAME);
        if (creds == null || !CredentialStore.MODE_XTREAM.equals(creds.mode)
                || id == null || !id.matches("\\d{1,12}")) {
            finish();
            return;
        }
        seriesName = name == null ? "" : name;

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

        loadEpisodes(id);
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
        showSeason(0);
        if (seasons.size() == 1) {
            lvEpisodes.requestFocus();
        } else {
            lvSeasons.requestFocus();
        }
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

    private void playEpisode(int position) {
        if (position < 0 || position >= shown.size()) {
            return;
        }
        XtreamApi.Episode chosen = shown.get(position);

        // Fila de reprodução: temporadas normais em ordem, e os "Especiais" (temporada 0) no fim.
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

        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(HomeActivity.EXTRA_KIND, XtreamApi.Kind.EPISODE.name());
        i.putExtra(HomeActivity.EXTRA_ID, chosen.id);
        i.putExtra(HomeActivity.EXTRA_EXT, chosen.ext);
        i.putExtra(HomeActivity.EXTRA_NAME, episodeLabel(chosen));
        i.putStringArrayListExtra(EXTRA_LIST_IDS, ids);
        i.putStringArrayListExtra(EXTRA_LIST_EXTS, exts);
        i.putExtra(EXTRA_LIST_INDEX, index);
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
            tv.setText(episodeLabel(shown.get(position)));
            return tv;
        }
    }
}

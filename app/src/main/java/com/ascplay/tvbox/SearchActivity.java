package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultados da busca por nome. Recebe o tipo (EXTRA_KIND: LIVE, MOVIE ou SERIES) e o texto
 * digitado (EXTRA_QUERY). Reaproveita o layout do "Ver mais" e esconde a lista de categorias.
 *
 * Servidor (Xtream): M11c usa o SearchIndex (arquivo no aparelho). A primeira busca de cada
 * tipo monta o arquivo (demora mais); as seguintes leem só o arquivo. Se o índice falhar e
 * ainda não há resultado, volta para XtreamApi.searchStream (busca antiga).
 * Lista M3U: M3uApi.searchStream, linha por linha. No máximo 100 resultados.
 * Os resultados são juntados em segundo plano e aparecem TODOS DE UMA VEZ quando a
 * busca termina (mostrar um a um fazia as capas piscarem). Voltar para a tela cancela a busca.
 * Controle parental ligado: itens adultos não aparecem.
 *
 * Ao escolher um resultado: canal toca direto; filme abre a página do filme (MovieActivity);
 * série abre a tela da série (levando a capa do item).
 * Canal ao vivo: o player pode trocar de canal com cima e baixo (R4). Esta tela manda
 * EXTRA_QUEUE = true e nenhuma categoria (nada = todas as categorias).
 * O texto da busca nunca é registrado em log.
 */
public class SearchActivity extends Activity {

    public static final String EXTRA_QUERY = "query";

    private static final int LIMIT = 100;
    private static final int MAX_QUERY = 40;
    private static final int COLOR_NEUTRAL = 0xFFA39A9D;
    private static final int COLOR_ERROR = 0xFFFF6B78;

    private CredentialStore.Credentials creds;
    private XtreamApi.Kind kind;
    private String query = "";

    private TextView tvTitle;
    private TextView tvStatus;
    private GridView gvItems;

    private final List<XtreamApi.Item> items = new ArrayList<>();
    private ItemAdapter itemAdapter;

    // Cancela a busca ao fechar a tela (lido pela thread da busca)
    private volatile boolean cancelled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        creds = CredentialStore.load(this);
        try {
            kind = XtreamApi.Kind.valueOf(getIntent().getStringExtra(HomeActivity.EXTRA_KIND));
        } catch (Exception e) {
            kind = null;
        }
        String q = getIntent().getStringExtra(EXTRA_QUERY);
        boolean xtream = creds != null && CredentialStore.MODE_XTREAM.equals(creds.mode);
        boolean m3u = creds != null && CredentialStore.MODE_M3U.equals(creds.mode);
        if ((!xtream && !m3u) || q == null || q.trim().isEmpty() || q.length() > MAX_QUERY
                || kind == null || kind == XtreamApi.Kind.EPISODE) {
            finish();
            return;
        }
        query = q.trim();

        setContentView(R.layout.activity_browse);
        tvTitle = findViewById(R.id.tv_title);
        tvStatus = findViewById(R.id.tv_status);
        gvItems = findViewById(R.id.gv_items);

        // A busca não tem categorias: esconde a lista da esquerda
        ListView lvCategories = findViewById(R.id.lv_categories);
        lvCategories.setVisibility(View.GONE);

        gvItems.setHorizontalSpacing(dp(10));
        gvItems.setVerticalSpacing(dp(10));
        itemAdapter = new ItemAdapter();
        gvItems.setAdapter(itemAdapter);
        gvItems.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < items.size()) {
                openItem(items.get(position));
            }
        });

        tvTitle.setText(kindLabel() + ": " + query);
        showStatus("Buscando... na primeira vez pode demorar um pouco mais.", COLOR_NEUTRAL);
        startSearch();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelled = true;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clear();
    }

    // ---------- Busca ----------

    private void startSearch() {
        final boolean skipAdult = ParentalStore.isEnabled(this);
        final boolean xtream = CredentialStore.MODE_XTREAM.equals(creds.mode);
        final String server = creds.server;
        final String user = creds.user;
        final String pass = creds.pass;
        final String m3uUrl = creds.m3uUrl;
        final XtreamApi.Kind k = kind;
        final String q = query;
        new Thread(() -> {
            // Só esta thread mexe nesta lista; ela vai inteira para a tela no fim.
            final List<XtreamApi.Item> found = new ArrayList<>();
            boolean failed = false;
            XtreamApi.ResultListener listener = item -> {
                if (cancelled) {
                    return false; // tela fechada: para de procurar
                }
                found.add(item);
                return true;
            };
            try {
                if (xtream) {
                    try {
                        SearchIndex.search(getApplicationContext(), server, user, pass, k, q,
                                skipAdult, LIMIT, () -> cancelled, listener);
                    } catch (IOException indexError) {
                        // Índice falhou: se nada foi achado ainda, usa a busca antiga
                        if (!cancelled && found.isEmpty()) {
                            XtreamApi.searchStream(server, user, pass, k, q, skipAdult, LIMIT,
                                    listener);
                        } else {
                            throw indexError;
                        }
                    }
                } else {
                    M3uApi.searchStream(m3uUrl, k, q, skipAdult, LIMIT, listener);
                }
            } catch (Exception e) {
                // sem rede, servidor fora do ar ou resposta estranha: avisa abaixo
                failed = true;
            }
            final boolean fail = failed;
            runOnUiThread(() -> onDone(found, fail));
        }).start();
    }

    /** A busca terminou (ou falhou): mostra tudo de uma vez. */
    private void onDone(List<XtreamApi.Item> found, boolean failed) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (found.isEmpty()) {
            if (failed) {
                showStatus("Não foi possível buscar. Confira a internet e o login.", COLOR_ERROR);
            } else {
                showStatus("Nada encontrado com esse nome.", COLOR_NEUTRAL);
            }
            return;
        }
        items.clear();
        items.addAll(found);
        itemAdapter.notifyDataSetChanged();
        tvStatus.setVisibility(View.GONE);
        gvItems.setSelection(0);
        gvItems.requestFocus();
        if (failed) {
            Toast.makeText(this, "A busca parou antes do fim.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openItem(XtreamApi.Item item) {
        boolean m3u = CredentialStore.MODE_M3U.equals(creds.mode);
        if (item.kind == XtreamApi.Kind.SERIES) {
            Intent s = new Intent(this, m3u ? M3uSeriesActivity.class : SeriesActivity.class);
            s.putExtra(HomeActivity.EXTRA_ID, item.id);
            s.putExtra(HomeActivity.EXTRA_NAME, item.name);
            s.putExtra(MovieActivity.EXTRA_ICON, item.icon);
            startActivity(s);
            return;
        }
        // Filme: abre a página do filme (capa e sinopse). Canal ao vivo: toca direto.
        Intent i = new Intent(this,
                item.kind == XtreamApi.Kind.MOVIE ? MovieActivity.class : MainActivity.class);
        i.putExtra(HomeActivity.EXTRA_KIND, item.kind.name());
        i.putExtra(HomeActivity.EXTRA_ID, item.id);
        i.putExtra(HomeActivity.EXTRA_EXT, item.ext);
        i.putExtra(HomeActivity.EXTRA_NAME, item.name);
        if (item.kind == XtreamApi.Kind.MOVIE) {
            i.putExtra(MovieActivity.EXTRA_ICON, item.icon);
        }
        if (item.kind == XtreamApi.Kind.LIVE) {
            // R4: o player troca de canal com cima e baixo. Categoria não enviada = todas.
            i.putExtra(MainActivity.EXTRA_QUEUE, true);
        }
        if (m3u) {
            i.putExtra(MainActivity.EXTRA_URL, item.url);
        }
        startActivity(i);
    }

    // ---------- Ajudas ----------

    private String kindLabel() {
        switch (kind) {
            case MOVIE:
                return "Filmes";
            case SERIES:
                return "Séries";
            default:
                return "Canais";
        }
    }

    private void showStatus(String text, int color) {
        tvStatus.setTextColor(color);
        tvStatus.setText(text);
        tvStatus.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String firstLetter(String name) {
        String s = name == null ? "" : name.trim();
        if (s.isEmpty()) {
            return "?";
        }
        return new String(Character.toChars(s.codePointAt(0))).toUpperCase();
    }

    // ---------- Adaptador (reaproveita os cartões ao rolar) ----------

    private final class ItemAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View card = convertView;
            if (card == null) {
                card = LayoutInflater.from(SearchActivity.this)
                        .inflate(R.layout.item_card, parent, false);
                card.setFocusable(false);
                card.setClickable(false);
            }
            XtreamApi.Item item = items.get(position);
            ((TextView) card.findViewById(R.id.tv_name)).setText(item.name);
            ((TextView) card.findViewById(R.id.tv_initial)).setText(firstLetter(item.name));
            ImageLoader.load((ImageView) card.findViewById(R.id.iv_logo), item.icon);

            // Na busca não mostramos a linha "Agora"
            TextView now = card.findViewById(R.id.tv_now);
            now.setTag(null);
            now.setVisibility(View.GONE);
            return card;
        }
    }
}

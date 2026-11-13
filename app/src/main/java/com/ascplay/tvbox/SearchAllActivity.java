package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Busca geral (visual Glass): procura o texto em canais, filmes e séries e mostra os resultados
 * AGRUPADOS por tipo, uma linha para cada. Usa as mesmas funções da SearchActivity
 * (SearchIndex no servidor, M3uApi.searchStream na lista M3U), uma de cada vez, fora da thread
 * principal. Cada grupo aparece quando a sua busca termina (grupos diferentes, sem piscar).
 * No máximo 20 resultados por tipo. Controle parental ligado: itens adultos não aparecem.
 * O texto da busca nunca é registrado em log.
 */
public class SearchAllActivity extends Activity {

    private static final int LIMIT = 20;
    private static final int MAX_QUERY = 40;

    private CredentialStore.Credentials creds;
    private String owner = "";
    private String query = "";
    private volatile boolean cancelled = false;
    private ScrollView scroll;
    private boolean focused = false;
    private final SectionRow[] rows = new SectionRow[3];
    private final XtreamApi.Kind[] kinds = {
            XtreamApi.Kind.LIVE, XtreamApi.Kind.MOVIE, XtreamApi.Kind.SERIES};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        creds = CredentialStore.load(this);
        String q = getIntent().getStringExtra(SearchActivity.EXTRA_QUERY);
        boolean xtream = creds != null && CredentialStore.MODE_XTREAM.equals(creds.mode);
        boolean m3u = creds != null && CredentialStore.MODE_M3U.equals(creds.mode);
        if ((!xtream && !m3u) || q == null || q.trim().isEmpty() || q.length() > MAX_QUERY) {
            finish();
            return;
        }
        query = q.trim();
        owner = HomeStore.ownerOf(creds);

        FrameLayout root = new FrameLayout(this);
        root.addView(new GlassBackground(this, 0.30f), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        column.setPadding(GlassKit.dp(this, 24), GlassKit.dp(this, 20), GlassKit.dp(this, 24), 0);
        root.addView(column, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("Resultados para \"" + query + "\"");
        title.setTextSize(GlassKit.sp(22));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setSingleLine(true);
        title.setTextColor(GlassKit.color(this, R.color.asc_text_primary));
        column.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        scroll = new ScrollView(this);
        scroll.setFocusable(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        sl.topMargin = GlassKit.dp(this, 6);
        column.addView(scroll, sl);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setClipChildren(false);
        body.setPadding(0, 0, 0, GlassKit.dp(this, 28));
        scroll.addView(body, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        String[] titles = {"Canais", "Filmes", "Séries"};
        for (int i = 0; i < 3; i++) {
            rows[i] = new SectionRow(this, titles[i]);
            rows[i].viewAllButton().setVisibility(View.GONE);
            rows[i].showMessage("Buscando... na primeira vez pode demorar um pouco mais.");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = GlassKit.dp(this, 8);
            body.addView(rows[i], lp);
        }
        setContentView(root);
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

    private void startSearch() {
        final boolean skipAdult = ParentalStore.isEnabled(this);
        final boolean xtream = CredentialStore.MODE_XTREAM.equals(creds.mode);
        final String server = creds.server;
        final String user = creds.user;
        final String pass = creds.pass;
        final String m3uUrl = creds.m3uUrl;
        final String q = query;
        new Thread(() -> {
            for (int idx = 0; idx < 3 && !cancelled; idx++) {
                final XtreamApi.Kind k = kinds[idx];
                final List<XtreamApi.Item> found = new ArrayList<>();
                boolean failed = false;
                XtreamApi.ResultListener listener = item -> {
                    if (cancelled) {
                        return false;
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
                    failed = true;
                }
                final int row = idx;
                final boolean fail = failed;
                runOnUiThread(() -> showGroup(row, found, fail));
            }
        }).start();
    }

    private void showGroup(int idx, List<XtreamApi.Item> found, boolean failed) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        SectionRow row = rows[idx];
        if (found.isEmpty()) {
            if (failed) {
                row.showError("Não foi possível buscar. Confira a internet e o login.");
            } else {
                row.showMessage("Nada encontrado com esse nome.");
            }
            return;
        }
        final XtreamApi.Kind kind = kinds[idx];
        LinearLayout rail = row.rail();
        rail.removeAllViews();
        View first = null;
        for (final XtreamApi.Item item : found) {
            View card;
            if (kind == XtreamApi.Kind.LIVE) {
                ChannelCard cc = new ChannelCard(this, null);
                cc.bind(item.name, "Ao vivo", item.icon);
                rail.addView(cc, ChannelCard.params(this));
                card = cc;
            } else {
                PosterCard pc = new PosterCard(this, null);
                pc.bind(item.name, kind == XtreamApi.Kind.MOVIE ? "Filme" : "Série", item.icon);
                rail.addView(pc, PosterCard.params(this));
                card = pc;
            }
            card.setOnClickListener(v -> openItem(item));
            if (first == null) {
                first = card;
            }
        }
        row.setCount(found.size() + (found.size() == 1 ? " resultado" : " resultados"));
        row.showItems();
        if (!focused && first != null) {
            focused = true;
            first.requestFocus();
        }
    }

    private void openItem(final XtreamApi.Item item) {
        boolean m3u = CredentialStore.MODE_M3U.equals(creds.mode);
        if (item.kind == XtreamApi.Kind.SERIES) {
            Intent s = new Intent(this, m3u ? M3uSeriesActivity.class : SeriesActivity.class);
            s.putExtra(HomeActivity.EXTRA_ID, item.id);
            s.putExtra(HomeActivity.EXTRA_NAME, item.name);
            s.putExtra(MovieActivity.EXTRA_ICON, item.icon);
            startActivity(s);
            return;
        }
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
            i.putExtra(MainActivity.EXTRA_QUEUE, true);
            final String ownerNow = owner;
            new Thread(() -> HomeStore.saveLastLive(getApplicationContext(), ownerNow, item)).start();
        }
        if (m3u) {
            i.putExtra(MainActivity.EXTRA_URL, item.url);
        }
        startActivity(i);
    }
}

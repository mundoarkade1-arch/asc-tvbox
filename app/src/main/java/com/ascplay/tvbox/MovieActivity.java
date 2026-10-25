package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Página do filme: capa, título, linha de informações, sinopse e botão Tocar.
 *
 * Servidor (Xtream): pede capa e sinopse UMA vez (XtreamApi.details), numa thread.
 * Se der erro de rede, a página fica sem sinopse (o filme ainda toca).
 * Lista M3U: não pede nada. Mostra a capa do item e "Sem sinopse nesta lista".
 *
 * Só é aberta a partir de itens que já passaram pelo filtro do controle parental.
 * Não tem menu à esquerda (tela de passagem, como a de resultados da busca).
 *
 * O botão "Continuar de onde parei" entra na parte (e).
 */
public class MovieActivity extends Activity {

    /** Endereço da capa do item (já conhecido da lista). Pode vir vazio. */
    public static final String EXTRA_ICON = "icon";

    private static final int COLOR_NEUTRAL = 0xFFA39A9D;

    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvMeta;
    private TextView tvPlot;
    private Button btnPlay;

    private CredentialStore.Credentials creds;
    private String kindName;
    private String movieId;
    private String movieExt;
    private String movieName;
    private String movieUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        creds = CredentialStore.load(this);
        Intent in = getIntent();
        kindName = in.getStringExtra(HomeActivity.EXTRA_KIND);
        movieId = in.getStringExtra(HomeActivity.EXTRA_ID);
        movieExt = in.getStringExtra(HomeActivity.EXTRA_EXT);
        movieName = in.getStringExtra(HomeActivity.EXTRA_NAME);
        movieUrl = in.getStringExtra(MainActivity.EXTRA_URL);
        String icon = in.getStringExtra(EXTRA_ICON);

        if (creds == null || movieId == null) {
            finish();
            return;
        }
        if (kindName == null) {
            kindName = XtreamApi.Kind.MOVIE.name();
        }
        if (movieName == null) {
            movieName = "";
        }
        if (icon == null) {
            icon = "";
        }

        setContentView(R.layout.activity_movie);

        ivCover = findViewById(R.id.iv_cover);
        tvTitle = findViewById(R.id.tv_title);
        tvMeta = findViewById(R.id.tv_meta);
        tvPlot = findViewById(R.id.tv_plot);
        btnPlay = findViewById(R.id.btn_play);

        tvTitle.setText(movieName);
        ImageLoader.load(ivCover, icon);

        btnPlay.setOnClickListener(v -> play());
        btnPlay.requestFocus();

        if (CredentialStore.MODE_XTREAM.equals(creds.mode)) {
            tvPlot.setTextColor(COLOR_NEUTRAL);
            tvPlot.setText("Carregando...");
            loadDetails();
        } else {
            tvPlot.setTextColor(COLOR_NEUTRAL);
            tvPlot.setText("Sem sinopse nesta lista.");
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clear();
    }

    /** Pede capa e sinopse ao servidor (um filme só) e preenche a página. */
    private void loadDetails() {
        final String server = creds.server;
        final String user = creds.user;
        final String pass = creds.pass;
        final String id = movieId;
        new Thread(() -> {
            XtreamApi.Details details = null;
            try {
                details = XtreamApi.details(server, user, pass, XtreamApi.Kind.MOVIE, id);
            } catch (Exception e) {
                // sem rede ou resposta estranha: a página fica sem sinopse
            }
            final XtreamApi.Details result = details;
            runOnUiThread(() -> showDetails(result));
        }).start();
    }

    private void showDetails(XtreamApi.Details d) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (d == null || (d.plot.isEmpty() && d.meta.isEmpty() && d.cover.isEmpty())) {
            tvPlot.setText("Sem sinopse para este filme.");
            return;
        }
        if (!d.cover.isEmpty()) {
            ImageLoader.load(ivCover, d.cover);
        }
        if (!d.meta.isEmpty()) {
            tvMeta.setText(d.meta);
            tvMeta.setVisibility(View.VISIBLE);
        }
        if (d.plot.isEmpty()) {
            tvPlot.setText("Sem sinopse para este filme.");
        } else {
            tvPlot.setTextColor(0xFFE6DFE1);
            tvPlot.setText(d.plot);
        }
    }

    /** Abre o player, do mesmo jeito que o filme abria antes. */
    private void play() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(HomeActivity.EXTRA_KIND, kindName);
        i.putExtra(HomeActivity.EXTRA_ID, movieId);
        i.putExtra(HomeActivity.EXTRA_EXT, movieExt == null ? "" : movieExt);
        i.putExtra(HomeActivity.EXTRA_NAME, movieName);
        if (CredentialStore.MODE_M3U.equals(creds.mode) && movieUrl != null) {
            i.putExtra(MainActivity.EXTRA_URL, movieUrl);
        }
        startActivity(i);
    }
}

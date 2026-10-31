package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

/**
 * Página do filme: capa, título, linha de informações, sinopse e botões.
 *
 * Servidor (Xtream): pede capa e sinopse UMA vez (XtreamApi.details), numa thread.
 * Se der erro de rede, a página fica sem sinopse (o filme ainda toca).
 * Lista M3U: não pede nada. Mostra a capa do item e "Sem sinopse nesta lista".
 *
 * Botões: "Tocar" (sempre do começo) e "Continuar de onde parei" (só aparece se a pessoa já
 * assistiu uma parte e não terminou; o foco abre nele). O minuto vem do PlaybackProgress, que o
 * player (MainActivity) vai anotando. A conferência é refeita toda vez que a pessoa volta do
 * player (onResume).
 *
 * A capa (movieIcon) também é mandada ao player (MainActivity.EXTRA_ICON), para a lista
 * "Continue assistindo".
 *
 * Só é aberta a partir de itens que já passaram pelo filtro do controle parental.
 * Não tem menu à esquerda (tela de passagem, como a de resultados da busca).
 */
public class MovieActivity extends Activity {

    /** Endereço da capa do item (já conhecido da lista). Pode vir vazio. */
    public static final String EXTRA_ICON = "icon";

    private static final int COLOR_NEUTRAL = 0xFFA99FA3;

    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvMeta;
    private TextView tvPlot;
    private Button btnPlay;
    private Button btnContinue;

    private CredentialStore.Credentials creds;
    private String kindName;
    private String movieId;
    private String movieExt;
    private String movieName;
    private String movieUrl;
    // Capa do filme (a do item; trocada pela do servidor, se ela chegar)
    private String movieIcon = "";

    // Minuto onde a pessoa parou (0 = nada para continuar)
    private long resumeMs = 0L;

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
        movieIcon = icon;

        setContentView(R.layout.activity_movie);

        ivCover = findViewById(R.id.iv_cover);
        tvTitle = findViewById(R.id.tv_title);
        tvMeta = findViewById(R.id.tv_meta);
        tvPlot = findViewById(R.id.tv_plot);
        btnPlay = findViewById(R.id.btn_play);
        btnContinue = findViewById(R.id.btn_continue);

        tvTitle.setText(movieName);
        ImageLoader.load(ivCover, icon);

        btnPlay.setOnClickListener(v -> play(0L));
        btnContinue.setOnClickListener(v -> play(resumeMs));
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
    protected void onResume() {
        super.onResume();
        // Ao abrir e ao voltar do player: confere se há um minuto para continuar
        refreshContinue();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clear();
    }

    // ---------- Continuar de onde parei ----------

    /** Chave deste filme no arquivo de minutos (a mesma que o player usa). */
    private String progressKey() {
        if (creds == null || movieId == null) {
            return null;
        }
        if (CredentialStore.MODE_M3U.equals(creds.mode)) {
            if (movieUrl == null || movieUrl.isEmpty()) {
                return null;
            }
            return PlaybackProgress.urlKey(movieUrl);
        }
        return PlaybackProgress.movieKey(creds.server, creds.user, movieId);
    }

    /** Mostra o botão "Continuar" só se assistiu uma parte (e não terminou). */
    private void refreshContinue() {
        if (btnPlay == null || btnContinue == null) {
            return;
        }
        PlaybackProgress.Mark mark = null;
        String key = progressKey();
        if (key != null) {
            mark = PlaybackProgress.load(this, key);
        }
        if (mark != null && mark.isPartial()) {
            resumeMs = mark.positionMs;
            btnContinue.setText("Continuar de onde parei (" + clock(mark.positionMs) + ")");
            btnContinue.setVisibility(View.VISIBLE);
            btnContinue.requestFocus();
        } else {
            resumeMs = 0L;
            btnContinue.setVisibility(View.GONE);
            btnPlay.requestFocus();
        }
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

    // ---------- Capa e sinopse ----------

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
            movieIcon = d.cover;
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

    // ---------- Tocar ----------

    /** Abre o player. Se startMs for maior que zero, o filme começa naquele minuto. */
    private void play(long startMs) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(HomeActivity.EXTRA_KIND, kindName);
        i.putExtra(HomeActivity.EXTRA_ID, movieId);
        i.putExtra(HomeActivity.EXTRA_EXT, movieExt == null ? "" : movieExt);
        i.putExtra(HomeActivity.EXTRA_NAME, movieName);
        if (CredentialStore.MODE_M3U.equals(creds.mode) && movieUrl != null) {
            i.putExtra(MainActivity.EXTRA_URL, movieUrl);
        }
        if (movieIcon != null && !movieIcon.isEmpty()) {
            i.putExtra(MainActivity.EXTRA_ICON, movieIcon);
        }
        if (startMs > 0L) {
            i.putExtra(MainActivity.EXTRA_START_MS, startMs);
        }
        startActivity(i);
    }
}

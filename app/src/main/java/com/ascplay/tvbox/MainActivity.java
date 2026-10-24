package com.ascplay.tvbox;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Player: toca o canal, filme ou episódio escolhido.
 * No login por servidor (Xtream), o endereço (com usuário e senha) é montado aqui, a partir
 * do login salvo. No login por lista M3U, o endereço direto do vídeo chega pela tela anterior
 * (EXTRA_URL). Nunca mostra nem registra o endereço, porque ele contém a senha.
 *
 * Nos canais ao vivo do servidor Xtream, uma barra embaixo mostra o programa de agora, a barra
 * de progresso, os horários e o "A seguir". Ela aparece quando o canal começa a tocar,
 * some depois de alguns segundos e volta ao apertar OK. (Lista M3U não tem guia.)
 *
 * Nos episódios de série, a tela da série manda a fila de episódios da série toda, em ordem
 * (temporadas normais primeiro, "Especiais" no fim):
 * - servidor Xtream: lista de números (ids) e extensões;
 * - lista M3U: lista de endereços diretos (EXTRA_LIST_URLS).
 * Quando um episódio termina, o player toca sozinho o próximo da fila, inclusive passando
 * para a temporada seguinte. No último episódio da série mostra "Fim da série." e para.
 */
@OptIn(markerClass = UnstableApi.class)
public class MainActivity extends Activity {

    /** Endereço direto do vídeo (só no login por lista M3U). */
    public static final String EXTRA_URL = "url";
    /** Lista dos endereços diretos dos episódios da série (só na lista M3U). */
    public static final String EXTRA_LIST_URLS = "list_urls";

    private static final String USER_AGENT = "ASC-TVBox/1.0 (Android)";
    private static final int TIMEOUT_MS = 15000;
    private static final long NOWBAR_HIDE_MS = 6000L;
    private static final int MAX_LIST = 500;
    private static final int MAX_URL = 4096;

    private PlayerView playerView;
    private TextView status;
    private ExoPlayer player;

    private CredentialStore.Credentials creds;
    private XtreamApi.Item item;
    private String streamUrl;
    private boolean triedTs = false;

    // Fila de episódios (só quando está tocando um episódio de série)
    private ArrayList<String> listIds = null;
    private ArrayList<String> listExts = null;
    // Lista M3U: endereços diretos dos episódios da série
    private ArrayList<String> listUrls = null;
    private int listIndex = -1;

    // Barra Agora / A seguir
    private View groupNowbar;
    private TextView tvNbChannel;
    private TextView tvNbTitle;
    private TextView tvNbStart;
    private TextView tvNbEnd;
    private TextView tvNbNext;
    private ProgressBar pbNowbar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideNowbar = new Runnable() {
        @Override
        public void run() {
            groupNowbar.setVisibility(View.GONE);
        }
    };
    private boolean nowbarShownOnce = false;
    private int nowbarToken = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playerView = findViewById(R.id.player_view);
        status = findViewById(R.id.status);

        groupNowbar = findViewById(R.id.group_nowbar);
        tvNbChannel = findViewById(R.id.tv_nb_channel);
        tvNbTitle = findViewById(R.id.tv_nb_title);
        tvNbStart = findViewById(R.id.tv_nb_start);
        tvNbEnd = findViewById(R.id.tv_nb_end);
        tvNbNext = findViewById(R.id.tv_nb_next);
        pbNowbar = findViewById(R.id.pb_nowbar);

        creds = CredentialStore.load(this);
        boolean xtream = creds != null && CredentialStore.MODE_XTREAM.equals(creds.mode);
        boolean m3u = creds != null && CredentialStore.MODE_M3U.equals(creds.mode);
        if (!xtream && !m3u) {
            showStatus("Nada para tocar. Volte e escolha um canal ou filme.");
            return;
        }

        String kindName = getIntent().getStringExtra(HomeActivity.EXTRA_KIND);
        String id = getIntent().getStringExtra(HomeActivity.EXTRA_ID);
        String ext = getIntent().getStringExtra(HomeActivity.EXTRA_EXT);
        String name = getIntent().getStringExtra(HomeActivity.EXTRA_NAME);
        if (kindName == null || id == null || !id.matches("\\d{1,12}")) {
            showStatus("Nada para tocar. Volte e escolha um canal ou filme.");
            return;
        }
        XtreamApi.Kind kind;
        try {
            kind = XtreamApi.Kind.valueOf(kindName);
        } catch (IllegalArgumentException e) {
            showStatus("Nada para tocar. Volte e escolha um canal ou filme.");
            return;
        }
        if (ext == null || !ext.matches("[A-Za-z0-9]{0,5}")) {
            ext = "";
        }

        // Lista M3U: o endereço do vídeo já vem pronto e precisa ser http ou https
        String url = "";
        if (m3u) {
            url = getIntent().getStringExtra(EXTRA_URL);
            if (!isHttp(url) || url.length() > MAX_URL) {
                showStatus("Nada para tocar. Volte e escolha um canal ou filme.");
                return;
            }
        }

        item = new XtreamApi.Item(kind, id, name == null ? "" : name, "", "", "", ext, url);
        streamUrl = XtreamApi.streamUrl(creds.server, creds.user, creds.pass, item);

        if (kind == XtreamApi.Kind.EPISODE) {
            if (xtream) {
                readEpisodeList();
            } else {
                readM3uEpisodeList();
            }
        }
    }

    private static boolean isHttp(String s) {
        return s != null && (s.regionMatches(true, 0, "http://", 0, 7)
                || s.regionMatches(true, 0, "https://", 0, 8));
    }

    /**
     * Lê a fila de episódios da série que a tela da série (servidor) mandou.
     * Se algo vier errado, não usa a fila: o episódio escolhido toca normalmente
     * e só não passa para o próximo sozinho.
     */
    private void readEpisodeList() {
        ArrayList<String> ids = getIntent().getStringArrayListExtra(SeriesActivity.EXTRA_LIST_IDS);
        ArrayList<String> exts = getIntent().getStringArrayListExtra(SeriesActivity.EXTRA_LIST_EXTS);
        int index = getIntent().getIntExtra(SeriesActivity.EXTRA_LIST_INDEX, -1);
        if (ids == null || exts == null || ids.size() != exts.size()
                || ids.isEmpty() || ids.size() > MAX_LIST
                || index < 0 || index >= ids.size()) {
            return;
        }
        for (int i = 0; i < ids.size(); i++) {
            String eid = ids.get(i);
            String eext = exts.get(i);
            if (eid == null || !eid.matches("\\d{1,12}")
                    || eext == null || !eext.matches("[A-Za-z0-9]{0,5}")) {
                return;
            }
        }
        listIds = ids;
        listExts = exts;
        listIndex = index;
    }

    /**
     * Lê a fila de endereços dos episódios da série que a tela da série (lista M3U) mandou.
     * Cada endereço precisa ser http ou https. Se algo vier errado, não usa a fila: o episódio
     * escolhido toca normalmente e só não passa para o próximo sozinho.
     */
    private void readM3uEpisodeList() {
        ArrayList<String> urls = getIntent().getStringArrayListExtra(EXTRA_LIST_URLS);
        int index = getIntent().getIntExtra(SeriesActivity.EXTRA_LIST_INDEX, -1);
        if (urls == null || urls.isEmpty() || urls.size() > MAX_LIST
                || index < 0 || index >= urls.size()) {
            return;
        }
        for (int i = 0; i < urls.size(); i++) {
            String u = urls.get(i);
            if (!isHttp(u) || u.length() > MAX_URL) {
                return;
            }
        }
        listUrls = urls;
        listIndex = index;
    }

    @Override
    protected void onStart() {
        super.onStart();
        startPlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(hideNowbar);
        nowbarToken++;
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(hideNowbar);
        nowbarToken++;
    }

    /** Ao apertar OK num canal ao vivo, mostra a barra Agora / A seguir. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            int code = event.getKeyCode();
            if (code == KeyEvent.KEYCODE_DPAD_CENTER
                    || code == KeyEvent.KEYCODE_ENTER
                    || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                showNowbar();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void startPlayer() {
        if (player != null || streamUrl == null) {
            return;
        }
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(TIMEOUT_MS)
                .setReadTimeoutMs(TIMEOUT_MS);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(http))
                .build();
        playerView.setPlayer(player);
        playerView.requestFocus();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    showStatus("Carregando...");
                } else if (state == Player.STATE_READY) {
                    status.setVisibility(View.GONE);
                    if (!nowbarShownOnce) {
                        nowbarShownOnce = true;
                        showNowbar();
                    }
                } else if (state == Player.STATE_ENDED) {
                    // Terminou um episódio: toca o próximo da fila
                    if (item != null && item.kind == XtreamApi.Kind.EPISODE) {
                        playNextEpisode();
                    }
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                // Canal ao vivo do servidor: se não entregou HLS (.m3u8), tenta uma vez o .ts.
                // (Na lista M3U o endereço é o que veio na lista: não mexemos nele.)
                if (item != null && item.kind == XtreamApi.Kind.LIVE && item.url.isEmpty()
                        && !triedTs && streamUrl != null && streamUrl.endsWith(".m3u8")) {
                    triedTs = true;
                    streamUrl = streamUrl.substring(0, streamUrl.length() - ".m3u8".length()) + ".ts";
                    playCurrent();
                    return;
                }
                // Não mostra a mensagem completa: ela pode conter o endereço com a senha.
                showStatus("Não foi possível tocar (" + error.getErrorCodeName() + ").");
            }
        });

        playCurrent();
    }

    private void playCurrent() {
        if (player == null || streamUrl == null) {
            return;
        }
        showStatus("Carregando...");
        player.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)));
        player.prepare();
        player.setPlayWhenReady(true);
    }

    /**
     * Passa para o próximo episódio da fila. A fila tem a série toda, então passa sozinho
     * para a temporada seguinte. No último episódio da série avisa "Fim da série.".
     */
    private void playNextEpisode() {
        int next = listIndex + 1;

        // Lista M3U: a lista tem os endereços prontos
        if (listUrls != null) {
            if (next >= listUrls.size()) {
                showStatus("Fim da série.");
                return;
            }
            listIndex = next;
            item = new XtreamApi.Item(XtreamApi.Kind.EPISODE, String.valueOf(next + 1), "", "",
                    "", "", "", listUrls.get(next));
            streamUrl = XtreamApi.streamUrl(creds.server, creds.user, creds.pass, item);
            playCurrent();
            return;
        }

        // Servidor: a lista tem números e extensões, e o endereço é montado aqui
        if (listIds == null || listExts == null || next >= listIds.size()) {
            showStatus("Fim da série.");
            return;
        }
        listIndex = next;
        item = new XtreamApi.Item(XtreamApi.Kind.EPISODE, listIds.get(next), "", "", "", "",
                listExts.get(next));
        streamUrl = XtreamApi.streamUrl(creds.server, creds.user, creds.pass, item);
        playCurrent();
    }

    private void releasePlayer() {
        if (player != null) {
            playerView.setPlayer(null);
            player.release();
            player = null;
        }
    }

    private void showStatus(String text) {
        status.setText(text);
        status.setVisibility(View.VISIBLE);
    }

    // ---------- Barra Agora / A seguir ----------

    private void showNowbar() {
        if (item == null || creds == null || item.kind != XtreamApi.Kind.LIVE
                || !CredentialStore.MODE_XTREAM.equals(creds.mode)) {
            return;
        }
        handler.removeCallbacks(hideNowbar);
        final int token = ++nowbarToken;

        tvNbChannel.setText(item.name);
        tvNbTitle.setText("Carregando...");
        tvNbStart.setText("");
        tvNbEnd.setText("");
        tvNbNext.setText("");
        pbNowbar.setProgress(0);
        groupNowbar.setVisibility(View.VISIBLE);
        handler.postDelayed(hideNowbar, NOWBAR_HIDE_MS);

        EpgCache.get(creds.server, creds.user, creds.pass, item.id, epg -> {
            if (isFinishing() || isDestroyed() || token != nowbarToken) {
                return;
            }
            fillNowbar(epg);
        });
    }

    private void fillNowbar(XtreamApi.Epg epg) {
        if (epg == null) {
            tvNbTitle.setText("Programação indisponível");
            tvNbStart.setText("");
            tvNbEnd.setText("");
            tvNbNext.setText("");
            pbNowbar.setProgress(0);
            return;
        }
        tvNbTitle.setText(epg.title);
        tvNbStart.setText(clock(epg.start));
        tvNbEnd.setText(clock(epg.end));

        long total = epg.end - epg.start;
        long passed = System.currentTimeMillis() - epg.start;
        int percent = 0;
        if (total > 0) {
            percent = (int) ((passed * 100L) / total);
        }
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }
        pbNowbar.setProgress(percent);

        if (epg.nextTitle == null || epg.nextTitle.isEmpty()) {
            tvNbNext.setText("");
        } else if (epg.nextStart > 0) {
            tvNbNext.setText("A seguir: " + clock(epg.nextStart) + "  " + epg.nextTitle);
        } else {
            tvNbNext.setText("A seguir: " + epg.nextTitle);
        }
    }

    private static String clock(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(millis));
    }
}

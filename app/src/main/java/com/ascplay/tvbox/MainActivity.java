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
import java.util.Date;
import java.util.Locale;

/**
 * Player: toca o canal ou filme escolhido na página inicial.
 * O endereço (com usuário e senha) é montado aqui, a partir do login salvo.
 * Nunca mostra nem registra o endereço, porque ele contém a senha.
 *
 * Nos canais ao vivo, uma barra embaixo mostra o programa de agora, a barra de
 * progresso, os horários e o "A seguir". Ela aparece quando o canal começa a tocar,
 * some depois de alguns segundos e volta ao apertar OK.
 */
@OptIn(markerClass = UnstableApi.class)
public class MainActivity extends Activity {

    private static final String USER_AGENT = "ASC-TVBox/1.0 (Android)";
    private static final int TIMEOUT_MS = 15000;
    private static final long NOWBAR_HIDE_MS = 6000L;

    private PlayerView playerView;
    private TextView status;
    private ExoPlayer player;

    private CredentialStore.Credentials creds;
    private XtreamApi.Item item;
    private String streamUrl;
    private boolean triedTs = false;

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
        if (creds == null || !CredentialStore.MODE_XTREAM.equals(creds.mode)) {
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
        item = new XtreamApi.Item(kind, id, name == null ? "" : name, "", "", "", ext);
        streamUrl = XtreamApi.streamUrl(creds.server, creds.user, creds.pass, item);
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
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                // Canal ao vivo: se o servidor não entregou HLS (.m3u8), tenta uma vez o .ts.
                if (item != null && item.kind == XtreamApi.Kind.LIVE && !triedTs
                        && streamUrl != null && streamUrl.endsWith(".m3u8")) {
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
        if (item == null || creds == null || item.kind != XtreamApi.Kind.LIVE) {
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

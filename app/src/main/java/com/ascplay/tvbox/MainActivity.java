package com.ascplay.tvbox;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
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

/**
 * Player: toca o canal ou filme escolhido na página inicial.
 * O endereço (com usuário e senha) é montado aqui, a partir do login salvo.
 * Nunca mostra nem registra o endereço, porque ele contém a senha.
 */
@OptIn(markerClass = UnstableApi.class)
public class MainActivity extends Activity {

    private static final String USER_AGENT = "ASC-TVBox/1.0 (Android)";
    private static final int TIMEOUT_MS = 15000;

    private PlayerView playerView;
    private TextView status;
    private ExoPlayer player;

    private CredentialStore.Credentials creds;
    private XtreamApi.Item item;
    private String streamUrl;
    private boolean triedTs = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playerView = findViewById(R.id.player_view);
        status = findViewById(R.id.status);

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
        releasePlayer();
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
}

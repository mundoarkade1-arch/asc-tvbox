package com.ascplay.tvbox;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends Activity {

    // Canal de teste público (stream HLS de demonstração da Apple).
    private static final String TEST_URL =
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8";

    private PlayerView playerView;
    private TextView status;
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playerView = findViewById(R.id.player_view);
        status = findViewById(R.id.status);
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
        if (player != null) {
            return;
        }
        player = new ExoPlayer.Builder(this).build();
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
                showStatus("Erro: " + error.getErrorCodeName() + " - " + error.getMessage());
            }
        });

        showStatus("Carregando...");
        player.setMediaItem(MediaItem.fromUri(TEST_URL));
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

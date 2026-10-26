package com.ascplay.tvbox;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
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
 * A tecla das três listras (ou segurar o OK) abre a janela com a programação do canal
 * (o que passa agora e os próximos programas), pedida ao servidor só nessa hora.
 *
 * Canal ao vivo: seta para baixo vai ao próximo canal e seta para cima ao anterior (R4).
 * A fila de canais (ChannelQueue) segue a ordem das categorias, começando no canal que toca.
 * A tela que abriu o canal manda EXTRA_QUEUE (true) e EXTRA_QUEUE_CAT (categoria de onde
 * veio; nulo = todas). Sem EXTRA_QUEUE, as setas cima e baixo não fazem nada de novo.
 *
 * Nos episódios de série, a tela da série manda a fila de episódios da série toda, em ordem
 * (temporadas normais primeiro, "Especiais" no fim):
 * - servidor Xtream: lista de números (ids) e extensões;
 * - lista M3U: lista de endereços diretos (EXTRA_LIST_URLS).
 * Quando um episódio termina, o player toca sozinho o próximo da fila, inclusive passando
 * para a temporada seguinte. No último episódio da série mostra "Fim da série." e para.
 * Faltando 2 minutos para acabar o episódio, aparece o botão "Pular para o próximo episódio"
 * (embaixo, à direita). OK nele pula para o próximo e marca este como visto.
 *
 * Quando um episódio começa a tocar, anota a posição dele na fila (SeriesProgress), para a
 * tela da série abrir no episódio certo da próxima vez. A chave da série vem em EXTRA_SERIES_KEY.
 *
 * Filme e episódio: anota o MINUTO exato (PlaybackProgress) a cada 10 segundos, ao sair da tela
 * e ao terminar (marca como visto). Canal ao vivo não anota nada. O recado EXTRA_START_MS faz o
 * primeiro vídeo começar naquele minuto ("Continuar de onde parei").
 *
 * Controles de filme e episódio (o foco não fica preso nos botões):
 * - OK pausa e continua, mesmo com a barra de controles escondida;
 * - seta direita/esquerda, um toque: avança/volta 10 segundos;
 * - seta direita/esquerda segurada: vai avançando/voltando enquanto segura, cada vez mais rápido.
 * A barra de controles fica semi-transparente.
 */
@OptIn(markerClass = UnstableApi.class)
public class MainActivity extends Activity {

    /** Endereço direto do vídeo (só no login por lista M3U). */
    public static final String EXTRA_URL = "url";
    /** Lista dos endereços diretos dos episódios da série (só na lista M3U). */
    public static final String EXTRA_LIST_URLS = "list_urls";
    /** Chave que identifica a série (usada só para lembrar onde a pessoa parou). */
    public static final String EXTRA_SERIES_KEY = "series_key";
    /** Minuto (em milissegundos) onde o primeiro vídeo deve começar. Sem isso, começa do zero. */
    public static final String EXTRA_START_MS = "start_ms";
    /** Verdadeiro: o canal ao vivo pode trocar de canal com as setas cima e baixo. */
    public static final String EXTRA_QUEUE = "channel_queue";
    /** Categoria de onde o canal veio (servidor: número; lista M3U: nome). Nulo = todas. */
    public static final String EXTRA_QUEUE_CAT = "channel_queue_cat";

    private static final String USER_AGENT = "ASC-TVBox/1.0 (Android)";
    private static final int TIMEOUT_MS = 15000;
    private static final long NOWBAR_HIDE_MS = 6000L;
    private static final long SAVE_EVERY_MS = 10000L;
    private static final long MIN_START_MS = 5000L;
    private static final long MAX_START_MS = 100L * 3600L * 1000L;
    // Setas esquerda e direita: um toque pula 10 s. Segurando, pula a cada HOLD_GAP_MS,
    // com passos maiores quanto mais tempo a tecla fica apertada.
    private static final long SEEK_MS = 10000L;
    private static final long HOLD_GAP_MS = 250L;
    private static final long HOLD_STEP1_MS = 10000L;   // primeiros 2 segundos segurando
    private static final long HOLD_STEP2_MS = 30000L;   // de 2 a 5 segundos segurando
    private static final long HOLD_STEP3_MS = 60000L;   // depois de 5 segundos segurando
    private static final long HOLD_PHASE1_MS = 2000L;
    private static final long HOLD_PHASE2_MS = 5000L;
    // Botão "Pular para o próximo episódio": aparece quando faltam 2 minutos
    private static final long SKIP_BEFORE_END_MS = 120000L;
    private static final long SKIP_CHECK_MS = 1000L;
    // Quanto a barra de controles fica visível (1.0 = nada transparente)
    private static final float CONTROLLER_ALPHA = 0.65f;
    private static final int MAX_LIST = 500;
    private static final int MAX_URL = 4096;
    private static final int MAX_KEY = 8192;

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

    // Troca de canal com as setas (só canal ao vivo)
    private ChannelQueue channelQueue = null;

    // Lembrar onde parou
    private String seriesKey = null;
    private int savedIndex = -1;

    // Minuto onde o próximo vídeo deve começar (0 = do começo). Usado uma vez.
    private long pendingStartMs = 0L;

    // Pulos com as setas: hora do último pulo e hora em que a tecla começou a ser segurada
    private long lastSeekAt = 0L;
    private long holdStartAt = 0L;

    // Botão "Pular para o próximo episódio" (criado no código, começa escondido)
    private TextView skipButton;

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
    // De tempos em tempos anota o minuto do filme ou episódio
    private final Runnable saveTick = new Runnable() {
        @Override
        public void run() {
            saveMark();
            handler.postDelayed(this, SAVE_EVERY_MS);
        }
    };
    // De tempos em tempos confere se já é hora de mostrar o botão de pular episódio
    private final Runnable skipTick = new Runnable() {
        @Override
        public void run() {
            updateSkipButton();
            handler.postDelayed(this, SKIP_CHECK_MS);
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

        makeControllerTransparent();
        createSkipButton();

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

        // "Continuar de onde parei": só filme e episódio começam no meio
        if (kind == XtreamApi.Kind.MOVIE || kind == XtreamApi.Kind.EPISODE) {
            long start = getIntent().getLongExtra(EXTRA_START_MS, 0L);
            if (start >= MIN_START_MS && start <= MAX_START_MS) {
                pendingStartMs = start;
            }
        }

        if (kind == XtreamApi.Kind.EPISODE) {
            if (xtream) {
                readEpisodeList();
            } else {
                readM3uEpisodeList();
            }
            String key = getIntent().getStringExtra(EXTRA_SERIES_KEY);
            if (key != null && !key.isEmpty() && key.length() <= MAX_KEY) {
                seriesKey = key;
            }
        }

        // Canal ao vivo: prepara a troca de canal com as setas cima e baixo
        if (kind == XtreamApi.Kind.LIVE && getIntent().getBooleanExtra(EXTRA_QUEUE, false)) {
            String cat = getIntent().getStringExtra(EXTRA_QUEUE_CAT);
            String currentKey = m3u ? url : id;
            channelQueue = new ChannelQueue(m3u, creds.server, creds.user, creds.pass,
                    creds.m3uUrl, cat, ParentalStore.isEnabled(this), currentKey,
                    new ChannelQueue.Listener() {
                        @Override
                        public void onChannel(XtreamApi.Item next) {
                            switchChannel(next);
                        }

                        @Override
                        public void onFail() {
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(MainActivity.this,
                                        "Não foi possível trocar de canal.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            channelQueue.start();
        }
    }

    /**
     * Deixa a barra de controles do player semi-transparente (dá para ver o vídeo por trás).
     * Se a barra não for achada, não faz nada e o player continua normal.
     */
    private void makeControllerTransparent() {
        int id = getResources().getIdentifier("exo_controller", "id", getPackageName());
        if (id == 0) {
            return;
        }
        View controller = playerView.findViewById(id);
        if (controller != null) {
            controller.setAlpha(CONTROLLER_ALPHA);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * Cria o botão pequeno "Pular para o próximo episódio" (embaixo, à direita, escondido).
     * Ele não recebe foco: o OK é tratado no dispatchKeyEvent.
     */
    private void createSkipButton() {
        skipButton = new TextView(this);
        skipButton.setText("Pular para o próximo episódio");
        skipButton.setTextColor(Color.WHITE);
        skipButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        skipButton.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC000000);
        bg.setCornerRadius(dp(6));
        bg.setStroke(dp(1), 0x99FFFFFF);
        skipButton.setBackground(bg);
        skipButton.setFocusable(false);
        skipButton.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        lp.setMargins(0, 0, dp(24), dp(96));
        addContentView(skipButton, lp);
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
        handler.removeCallbacks(saveTick);
        handler.removeCallbacks(skipTick);
        skipButton.setVisibility(View.GONE);
        nowbarToken++;
        // Anota o minuto e, se a pessoa voltar a esta tela, continua dali
        saveMark();
        rememberPositionForRestart();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(hideNowbar);
        handler.removeCallbacks(saveTick);
        handler.removeCallbacks(skipTick);
        nowbarToken++;
        if (channelQueue != null) {
            channelQueue.close();
            channelQueue = null;
        }
    }

    /**
     * Teclas do player.
     * - Filme e episódio: OK pausa e continua (ou, com o botão de pular episódio na tela,
     *   OK pula para o próximo episódio).
     * - Filme e episódio: seta direita/esquerda, um toque, avança/volta 10 segundos; segurando,
     *   vai avançando/voltando enquanto segura, cada vez mais rápido.
     * - Canal ao vivo: seta para baixo = próximo canal, seta para cima = canal anterior.
     * - Canal ao vivo: ao apertar OK, mostra a barra Agora / A seguir.
     * - Canal ao vivo do servidor: a tecla das três listras (menu), ou segurar o OK,
     *   abre a janela com a programação do canal.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        boolean isOk = code == KeyEvent.KEYCODE_DPAD_CENTER
                || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;

        // Tecla das três listras: programação do canal (só canal ao vivo do servidor)
        if (code == KeyEvent.KEYCODE_MENU && canShowSchedule()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                openSchedule();
            }
            return true;
        }

        // Canal ao vivo: cima e baixo trocam de canal
        if ((code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN)
                && canChangeChannel()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                    channelQueue.next();
                } else {
                    channelQueue.previous();
                }
            }
            return true;
        }

        // Filme e episódio: setas esquerda e direita
        if ((code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT)
                && canSeekWithArrows()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                long direction = code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1L : -1L;
                long now = SystemClock.elapsedRealtime();
                if (event.getRepeatCount() == 0) {
                    // Um toque: sempre pula 10 segundos
                    holdStartAt = now;
                    lastSeekAt = now;
                    seekBy(direction * SEEK_MS);
                } else if (now - lastSeekAt >= HOLD_GAP_MS) {
                    // Segurando: passos maiores quanto mais tempo segura
                    long held = now - holdStartAt;
                    long step;
                    if (held < HOLD_PHASE1_MS) {
                        step = HOLD_STEP1_MS;
                    } else if (held < HOLD_PHASE2_MS) {
                        step = HOLD_STEP2_MS;
                    } else {
                        step = HOLD_STEP3_MS;
                    }
                    lastSeekAt = now;
                    seekBy(direction * step);
                }
            }
            return true;
        }

        // Filme e episódio: OK pausa e continua (ou pula o episódio, se o botão está na tela)
        if (isOk && canSeekWithArrows()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (skipButton.getVisibility() == View.VISIBLE) {
                    skipToNextEpisode();
                } else {
                    togglePause();
                }
            }
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN && isOk) {
            if (event.getRepeatCount() == 0) {
                showNowbar();
            } else if (event.getRepeatCount() == 1) {
                // OK segurado: segunda via para abrir a programação
                openSchedule();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /** Só canal ao vivo, com a fila de canais pronta, troca de canal com as setas. */
    private boolean canChangeChannel() {
        return channelQueue != null && channelQueue.isReady() && item != null
                && item.kind == XtreamApi.Kind.LIVE;
    }

    /** Toca o canal novo escolhido pelas setas cima e baixo. */
    private void switchChannel(XtreamApi.Item next) {
        if (isFinishing() || isDestroyed() || next == null || creds == null || player == null) {
            return;
        }
        boolean m3u = CredentialStore.MODE_M3U.equals(creds.mode);
        String ext = next.ext;
        if (ext == null || !ext.matches("[A-Za-z0-9]{0,5}")) {
            ext = "";
        }
        String url = m3u && next.url != null ? next.url : "";
        item = new XtreamApi.Item(XtreamApi.Kind.LIVE, next.id, next.name == null ? "" : next.name,
                "", "", "", ext, url);
        streamUrl = XtreamApi.streamUrl(creds.server, creds.user, creds.pass, item);
        triedTs = false;
        playCurrent();
        if (m3u) {
            // Lista M3U não tem guia: mostra só o nome do canal
            Toast.makeText(this, item.name, Toast.LENGTH_SHORT).show();
        } else {
            showNowbar();
        }
    }

    /** A programação só existe para canal ao vivo do servidor (a lista M3U não tem guia). */
    private boolean canShowSchedule() {
        return item != null && creds != null && item.kind == XtreamApi.Kind.LIVE
                && CredentialStore.MODE_XTREAM.equals(creds.mode);
    }

    /** Abre a janela da programação do canal que está tocando. */
    private void openSchedule() {
        if (!canShowSchedule()) {
            return;
        }
        // Esconde a barra Agora / A seguir para não ficar uma por cima da outra
        handler.removeCallbacks(hideNowbar);
        nowbarToken++;
        groupNowbar.setVisibility(View.GONE);
        EpgDialog.show(this, creds.server, creds.user, creds.pass, item.id, item.name);
    }

    /** As setas e o OK só agem em filme e episódio que já estão carregando ou tocando. */
    private boolean canSeekWithArrows() {
        if (player == null || item == null) {
            return false;
        }
        if (item.kind != XtreamApi.Kind.MOVIE && item.kind != XtreamApi.Kind.EPISODE) {
            return false;
        }
        int state = player.getPlaybackState();
        return state != Player.STATE_IDLE && state != Player.STATE_ENDED;
    }

    /** Pula para frente (valor positivo) ou para trás (valor negativo) e mostra a barra. */
    private void seekBy(long deltaMs) {
        long target = player.getCurrentPosition() + deltaMs;
        if (target < 0) {
            target = 0;
        }
        long duration = player.getDuration();
        if (duration != C.TIME_UNSET && duration > 0 && target > duration) {
            target = duration;
        }
        player.seekTo(target);
        playerView.showController();
    }

    /** OK em filme e episódio: pausa se está tocando e continua se está pausado. */
    private void togglePause() {
        if (player.getPlayWhenReady()) {
            player.setPlayWhenReady(false);
        } else {
            player.setPlayWhenReady(true);
        }
        playerView.showController();
    }

    /** Existe um próximo episódio na fila? */
    private boolean hasNextEpisode() {
        int next = listIndex + 1;
        if (listUrls != null) {
            return next < listUrls.size();
        }
        return listIds != null && listExts != null && next < listIds.size();
    }

    /** Mostra o botão de pular quando faltam 2 minutos (ou menos) para acabar o episódio. */
    private void updateSkipButton() {
        boolean show = false;
        if (player != null && item != null && item.kind == XtreamApi.Kind.EPISODE
                && hasNextEpisode() && player.getPlaybackState() == Player.STATE_READY) {
            long duration = player.getDuration();
            if (duration != C.TIME_UNSET && duration > 0) {
                long left = duration - player.getCurrentPosition();
                show = left > 0 && left <= SKIP_BEFORE_END_MS;
            }
        }
        skipButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** OK no botão: marca este episódio como visto e toca o próximo. */
    private void skipToNextEpisode() {
        skipButton.setVisibility(View.GONE);
        if (item == null || item.kind != XtreamApi.Kind.EPISODE || !hasNextEpisode()) {
            return;
        }
        saveFinished();
        playNextEpisode();
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
                    saveProgress();
                    if (!nowbarShownOnce) {
                        nowbarShownOnce = true;
                        showNowbar();
                    }
                } else if (state == Player.STATE_ENDED) {
                    skipButton.setVisibility(View.GONE);
                    // Terminou um filme ou episódio: marca como visto
                    saveFinished();
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

        // Filme e episódio: anota o minuto de tempos em tempos
        handler.removeCallbacks(saveTick);
        handler.postDelayed(saveTick, SAVE_EVERY_MS);

        // Episódio: confere de tempos em tempos se já é hora do botão de pular
        handler.removeCallbacks(skipTick);
        handler.postDelayed(skipTick, SKIP_CHECK_MS);
    }

    private void playCurrent() {
        if (player == null || streamUrl == null) {
            return;
        }
        skipButton.setVisibility(View.GONE);
        showStatus("Carregando...");
        MediaItem media = MediaItem.fromUri(Uri.parse(streamUrl));
        if (pendingStartMs > 0) {
            // Continuar de onde parou: começa neste minuto (vale uma vez só)
            player.setMediaItem(media, pendingStartMs);
            pendingStartMs = 0L;
        } else {
            player.setMediaItem(media);
        }
        player.prepare();
        player.setPlayWhenReady(true);
    }

    /** Anota o episódio que começou a tocar (uma vez por episódio). */
    private void saveProgress() {
        if (item == null || item.kind != XtreamApi.Kind.EPISODE
                || seriesKey == null || listIndex < 0 || listIndex == savedIndex) {
            return;
        }
        savedIndex = listIndex;
        SeriesProgress.save(this, seriesKey, listIndex);
    }

    // ---------- Minuto exato (filme e episódio) ----------

    /** Chave do item que toca agora (resumo embaralhado é feito pelo PlaybackProgress). */
    private String progressKey() {
        if (item == null || creds == null || item.kind == XtreamApi.Kind.LIVE) {
            return null;
        }
        // Lista M3U: o endereço do vídeo identifica o item
        if (!item.url.isEmpty()) {
            return PlaybackProgress.urlKey(item.url);
        }
        if (item.kind == XtreamApi.Kind.MOVIE) {
            return PlaybackProgress.movieKey(creds.server, creds.user, item.id);
        }
        if (item.kind == XtreamApi.Kind.EPISODE) {
            return PlaybackProgress.episodeKey(creds.server, creds.user, item.id);
        }
        return null;
    }

    /** Anota o minuto atual do filme ou episódio. Não faz nada em canal ao vivo. */
    private void saveMark() {
        if (player == null) {
            return;
        }
        String key = progressKey();
        if (key == null) {
            return;
        }
        if (player.getPlaybackState() == Player.STATE_ENDED) {
            return; // o fim é anotado em saveFinished()
        }
        long duration = player.getDuration();
        if (duration == C.TIME_UNSET || duration <= 0) {
            return;
        }
        PlaybackProgress.save(this, key, player.getCurrentPosition(), duration);
    }

    /** Terminou o filme ou episódio: anota como visto (posição = duração). */
    private void saveFinished() {
        if (player == null) {
            return;
        }
        String key = progressKey();
        if (key == null) {
            return;
        }
        long duration = player.getDuration();
        if (duration == C.TIME_UNSET || duration <= 0) {
            return;
        }
        PlaybackProgress.save(this, key, duration, duration);
    }

    /** Se a pessoa sair e voltar a esta tela (por exemplo, Home), continua do mesmo minuto. */
    private void rememberPositionForRestart() {
        if (player == null || progressKey() == null) {
            return;
        }
        if (player.getPlaybackState() == Player.STATE_ENDED) {
            return;
        }
        long pos = player.getCurrentPosition();
        if (pos >= MIN_START_MS) {
            pendingStartMs = pos;
        }
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
        if (!canShowSchedule()) {
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

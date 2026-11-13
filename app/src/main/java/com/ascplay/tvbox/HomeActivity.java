package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Página inicial, visual "Glass UI" (vidro): menu lateral flutuante (SideRail), barra superior
 * (TopBar: busca, "Lista atualizada" e relógio), banner de destaque (HeroBanner) e as linhas
 * (SectionRow): Continue assistindo e Minha lista (só aparecem se houver itens), Canais ao vivo,
 * Filmes e Séries. Cada linha tem "Ver tudo", que abre a tela completa da seção.
 *
 * Só a interface mudou. Dados, login, player e reprodução continuam como antes: as mesmas chamadas
 * (XtreamApi.items, M3uApi.sample, EpgCache, ContinueStore, ParentalStore, Updater) e as mesmas
 * telas de destino (MovieActivity, SeriesActivity, M3uSeriesActivity, MainActivity, BrowseActivity,
 * M3uBrowseActivity, ContinueActivity, SettingsActivity).
 *
 * Novidades da Home: busca geral (SearchAllActivity), Minha lista (FavoritesStore), destaque que
 * troca a cada 8 s sem mexer no foco, card "AO VIVO AGORA" (último canal aberto, ou o primeiro),
 * chip com a data/hora da última atualização (HomeStore) e relógio 24h.
 *
 * Controle remoto: direita/esquerda dentro das linhas; cima/baixo entre busca, destaque e linhas.
 * O menu lateral é acessado pela esquerda (primeiro card de cada linha) e, apertando direita,
 * devolve o foco ao último item que estava focado. Voltar do player mantém o foco onde estava.
 *
 * Tecla de menu (três listras) sobre um canal ao vivo do servidor, ou OK segurado: programação.
 * O endereço do vídeo e a chave da série NUNCA são mostrados nem registrados em log.
 */
public class HomeActivity extends Activity {

    public static final String EXTRA_KIND = "kind";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_EXT = "ext";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_SHOW_LOGIN = "show_login";

    private static final int LIMIT = 10;
    private static final int HERO_CANDIDATES = 8;
    private static final int HERO_MAX = 5;
    private static final long HERO_PERIOD_MS = 8000L;

    // Ordem de prioridade do foco inicial (menor número = mais alto na tela)
    private static final int RANK_HERO = 0;
    private static final int RANK_CONTINUE = 1;
    private static final int RANK_FAV = 2;
    private static final int RANK_LIVE = 3;
    private static final int RANK_MOVIE = 4;
    private static final int RANK_SERIES = 5;

    private CredentialStore.Credentials creds;
    private String owner = "";

    // Componentes
    private SideRail rail;
    private TopBar topBar;
    private ScrollView scroll;
    private HeroBanner hero;
    private SectionRow rowContinue;
    private SectionRow rowFav;
    private SectionRow rowLive;
    private SectionRow rowMovie;
    private SectionRow rowSeries;

    // Aviso de atualização do app
    private GlassSurface updateBar;
    private TextView tvUpdate;
    private GlassSurface.Button btnUpdateNow;
    private GlassSurface.Button btnUpdateLater;
    private Updater.Info pendingUpdate;
    private boolean downloading = false;

    // Ignorar respostas antigas
    private int railGen = 0;
    private int continueGen = 0;
    private int favGen = 0;
    private volatile int heroGen = 0;

    // Estado do controle parental na última vez que as faixas foram montadas
    private boolean parentalSeen = false;

    // Atualização da lista: quantas faixas ainda faltam responder, e se alguma deu certo
    private int railsPending = 0;
    private boolean railsOk = false;

    // Foco
    private boolean userNavigated = false;
    private int autoFocusRank = Integer.MAX_VALUE;
    private View lastContentFocus;
    private int navHomeId = View.NO_ID;
    // Canal ao vivo (servidor) com o foco agora; só para a tecla de menu abrir a programação
    private XtreamApi.Item focusedLive;

    // Dados das faixas (já filtrados pelo controle parental)
    private final List<XtreamApi.Item> liveItems = new ArrayList<>();
    private final List<XtreamApi.Item> movieItems = new ArrayList<>();
    private final List<XtreamApi.Item> seriesItems = new ArrayList<>();
    private volatile Map<String, String> catNames = new HashMap<>();
    private final Map<String, PosterCard> posterByKey = new HashMap<>();
    private final Set<String> favKeys = new HashSet<>();

    // Destaque
    private static final class HeroInfo {
        String metaChip = "";
        String rating = "";
        double value = 0d;
        String cover = "";
    }

    private static final class HeroEntry {
        final XtreamApi.Item item;
        final HeroInfo info;

        HeroEntry(XtreamApi.Item item, HeroInfo info) {
            this.item = item;
            this.info = info;
        }
    }

    private final Map<String, HeroInfo> detailCache =
            Collections.synchronizedMap(new HashMap<String, HeroInfo>());
    private List<HeroEntry> heroEntries = new ArrayList<>();
    private int heroIndex = 0;
    private XtreamApi.Item currentHero;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean resumed = false;

    private final Runnable heroTick = new Runnable() {
        @Override
        public void run() {
            if (resumed && !isFinishing() && !isDestroyed()) {
                // Só troca se a pessoa não está com o foco no banner (não rouba nem muda o alvo)
                if (heroEntries.size() > 1 && !hero.hasFocusInside()) {
                    showHero((heroIndex + 1) % heroEntries.size());
                }
                handler.postDelayed(this, HERO_PERIOD_MS);
            }
        }
    };

    private final Runnable heroBuild = new Runnable() {
        @Override
        public void run() {
            buildHero();
        }
    };

    // ---------------------------------------------------------------------------------
    // Ciclo de vida
    // ---------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        creds = CredentialStore.load(this);
        if (creds == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        owner = HomeStore.ownerOf(creds);
        parentalSeen = ParentalStore.isEnabled(this);

        buildUi();
        wireFocus();
        wireClicks();

        topBar.setUpdatedText(updatedText(HomeStore.lastUpdated(this, owner)));
        loadAllRails();
        loadFavorites();
        checkForUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (creds == null) {
            return;
        }
        resumed = true;
        handler.removeCallbacks(heroTick);
        handler.postDelayed(heroTick, HERO_PERIOD_MS);
        topBar.setUpdatedText(updatedText(HomeStore.lastUpdated(this, owner)));

        // Voltou de outra tela (por exemplo, Ajustes): se o controle parental mudou, recarrega
        if (ParentalStore.isEnabled(this) != parentalSeen) {
            parentalSeen = ParentalStore.isEnabled(this);
            reloadRails();
        } else {
            // O "Continue assistindo", a Minha lista e o último canal podem ter mudado
            loadContinue();
            loadFavorites();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        handler.removeCallbacks(heroTick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        heroGen++;
        focusedLive = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clear();
        EpgCache.clear();
    }

    /**
     * Tecla das três listras (menu) sobre um canal ao vivo do servidor: abre a janela com a
     * programação do canal. Em qualquer outro lugar, a tecla não faz nada de novo.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int code = event.getKeyCode();
            if (code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                    || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                userNavigated = true;
            }
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && isXtream() && focusedLive != null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                openSchedule(focusedLive);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    // ---------------------------------------------------------------------------------
    // Montagem da tela
    // ---------------------------------------------------------------------------------

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.addView(new GlassBackground(this), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.HORIZONTAL);
        main.setClipChildren(false);
        root.addView(main, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Menu lateral flutuante: 32px das bordas no desenho = 16dp
        rail = new SideRail(this);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(
                GlassKit.dp(this, 76), LinearLayout.LayoutParams.MATCH_PARENT);
        rl.leftMargin = GlassKit.dp(this, 16);
        rl.topMargin = GlassKit.dp(this, 16);
        rl.bottomMargin = GlassKit.dp(this, 16);
        main.addView(rail, rl);

        // Coluna de conteúdo: margem de segurança (overscan) de 48px = 24dp
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        // sem isto a coluna corta o que passa do padding (banner até a borda e anel de foco)
        column.setClipToPadding(false);
        column.setPadding(GlassKit.dp(this, 16), GlassKit.dp(this, 20), GlassKit.dp(this, 24), 0);
        main.addView(column, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        topBar = new TopBar(this);
        column.addView(topBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        scroll = new ScrollView(this);
        scroll.setFocusable(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        column.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setClipChildren(false);
        body.setClipToPadding(false);
        body.setPadding(0, GlassKit.dp(this, 10), 0, GlassKit.dp(this, 28));
        scroll.addView(body, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        buildUpdateBar(body);

        hero = new HeroBanner(this);
        // O banner vai até a borda direita da tela (sem a faixa de margem da coluna)
        body.addView(hero, HeroBanner.params(this, GlassKit.dp(this, 24)));

        rowContinue = addRow(body, "Continue assistindo");
        rowContinue.setVisibility(View.GONE);
        rowFav = addRow(body, "Minha lista");
        rowFav.setVisibility(View.GONE);
        rowFav.viewAllButton().setVisibility(View.GONE);
        rowLive = addRow(body, "Canais ao vivo");
        rowMovie = addRow(body, "Filmes");
        rowSeries = addRow(body, "Séries");

        setContentView(root);
    }

    private SectionRow addRow(LinearLayout body, String title) {
        SectionRow row = new SectionRow(this, title);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = GlassKit.dp(this, 8);
        body.addView(row, lp);
        return row;
    }

    private void buildUpdateBar(LinearLayout body) {
        updateBar = new GlassSurface(this, GlassKit.dp(this, 16), false,
                GlassSurface.STYLE_GLASS, true);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(GlassKit.dp(this, 14), GlassKit.dp(this, 8), GlassKit.dp(this, 10),
                GlassKit.dp(this, 10));
        updateBar.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        tvUpdate = new TextView(this);
        tvUpdate.setTextSize(GlassKit.sp(15));
        tvUpdate.setTypeface(tvUpdate.getTypeface(), Typeface.BOLD);
        tvUpdate.setTextColor(GlassKit.color(this, R.color.asc_focus));
        row.addView(tvUpdate, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        btnUpdateNow = new GlassSurface.Button(this, getString(R.string.btn_update_now), true);
        row.addView(btnUpdateNow, new LinearLayout.LayoutParams(
                GlassKit.dp(this, 96), GlassKit.dp(this, 34)));
        btnUpdateLater = new GlassSurface.Button(this, getString(R.string.btn_update_later), false);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
                GlassKit.dp(this, 84), GlassKit.dp(this, 34));
        ll.leftMargin = GlassKit.dp(this, 10);
        row.addView(btnUpdateLater, ll);

        updateBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = GlassKit.dp(this, 8);
        body.addView(updateBar, lp);
    }

    /** Foco: esquerda leva ao menu; direita no menu volta ao último item focado. */
    private void wireFocus() {
        navHomeId = GlassKit.ensureId(rail.home);

        topBar.searchPill().setNextFocusLeftId(navHomeId);
        hero.playButton().setNextFocusLeftId(navHomeId);
        hero.playButton().setNextFocusUpId(GlassKit.ensureId(topBar.searchPill()));
        topBar.searchPill().setNextFocusDownId(GlassKit.ensureId(hero.playButton()));
        SectionRow[] rows = {rowContinue, rowFav, rowLive, rowMovie, rowSeries};
        for (SectionRow r : rows) {
            r.viewAllButton().setNextFocusLeftId(navHomeId);
        }

        // acompanhar o foco do topo e do banner (guarda o último item e rola para o topo)
        final View.OnFocusChangeListener heroTrack = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    lastContentFocus = v;
                    focusedLive = null;
                    scroll.smoothScrollTo(0, 0);
                }
            }
        };
        GlassKit.focusScale(topBar.searchPill(), 1.05f, heroTrack);
        GlassKit.focusScale(hero.playButton(), 1.08f, heroTrack);
        GlassKit.focusScale(hero.listButton(), 1.08f, heroTrack);

        for (SectionRow r : rows) {
            final SectionRow row = r;
            GlassKit.focusScale(r.viewAllButton(), 1.08f, track(row, null));
        }
        GlassKit.focusScale(btnUpdateNow, 1.08f, null);
        GlassKit.focusScale(btnUpdateLater, 1.08f, null);

        rail.setOnRight(new Runnable() {
            @Override
            public void run() {
                View target = lastContentFocus;
                if (target == null || !target.isAttachedToWindow() || !target.isShown()
                        || !target.isFocusable()) {
                    target = hero.playButton().isFocusable() ? hero.playButton()
                            : topBar.searchPill();
                }
                target.requestFocus();
            }
        });
    }

    private void wireClicks() {
        rail.home.setOnClickListener(v -> goToFirstCard());
        rail.live.setOnClickListener(v -> openSection(XtreamApi.Kind.LIVE));
        rail.movie.setOnClickListener(v -> openSection(XtreamApi.Kind.MOVIE));
        rail.series.setOnClickListener(v -> openSection(XtreamApi.Kind.SERIES));
        rail.settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        rail.refresh.setOnClickListener(v -> reloadRails());

        topBar.searchPill().setOnClickListener(v -> openSearch());

        hero.playButton().setOnClickListener(v -> {
            if (currentHero != null) {
                openItem(currentHero);
            }
        });
        hero.listButton().setOnClickListener(v -> toggleHeroFavorite());

        rowContinue.viewAllButton().setOnClickListener(
                v -> startActivity(new Intent(this, ContinueActivity.class)));
        rowLive.viewAllButton().setOnClickListener(v -> openBrowse(XtreamApi.Kind.LIVE));
        rowMovie.viewAllButton().setOnClickListener(v -> openBrowse(XtreamApi.Kind.MOVIE));
        rowSeries.viewAllButton().setOnClickListener(v -> openBrowse(XtreamApi.Kind.SERIES));

        btnUpdateNow.setOnClickListener(v -> startUpdate());
        btnUpdateLater.setOnClickListener(v -> dismissUpdate());
    }

    /** Guarda o último foco do conteúdo e garante que a linha apareça inteira ao subir. */
    private View.OnFocusChangeListener track(final SectionRow row, final XtreamApi.Item live) {
        return new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    lastContentFocus = v;
                    focusedLive = live;
                    if (row != null) {
                        ensureRowVisible(row);
                    }
                }
            }
        };
    }

    private void ensureRowVisible(final SectionRow row) {
        scroll.post(new Runnable() {
            @Override
            public void run() {
                int top = row.getTop() - GlassKit.dp(HomeActivity.this, 8);
                if (scroll.getScrollY() > top) {
                    scroll.smoothScrollTo(0, Math.max(0, top));
                }
            }
        });
    }

    /** Foco inicial automático: só sobe (para algo mais alto na tela), nunca desce. */
    private void autoFocus(int rank, View target) {
        if (target == null || userNavigated || rank >= autoFocusRank
                || updateBar.getVisibility() == View.VISIBLE || !target.isFocusable()) {
            return;
        }
        autoFocusRank = rank;
        target.requestFocus();
    }

    /** Início: leva o foco ao destaque (ou ao primeiro card) e rola para o topo. */
    private void goToFirstCard() {
        scroll.smoothScrollTo(0, 0);
        if (hero.playButton().isFocusable() && hero.playButton().getVisibility() == View.VISIBLE) {
            hero.playButton().requestFocus();
            return;
        }
        SectionRow[] rows = {rowContinue, rowFav, rowLive, rowMovie, rowSeries};
        for (SectionRow r : rows) {
            if (r.getVisibility() == View.VISIBLE && r.hasCards()) {
                r.firstCard().requestFocus();
                return;
            }
        }
        topBar.searchPill().requestFocus();
    }

    // ---------------------------------------------------------------------------------
    // Tipo de login
    // ---------------------------------------------------------------------------------

    private boolean isXtream() {
        return creds != null && CredentialStore.MODE_XTREAM.equals(creds.mode);
    }

    private boolean isM3u() {
        return creds != null && CredentialStore.MODE_M3U.equals(creds.mode);
    }

    // ---------------------------------------------------------------------------------
    // Faixas de canais, filmes e séries
    // ---------------------------------------------------------------------------------

    /** Busca as três faixas, de acordo com o tipo de login. */
    private void loadAllRails() {
        if (isXtream()) {
            beginRefreshState(3);
            loadRail(XtreamApi.Kind.LIVE, rowLive);
            loadRail(XtreamApi.Kind.MOVIE, rowMovie);
            loadRail(XtreamApi.Kind.SERIES, rowSeries);
        } else if (isM3u()) {
            beginRefreshState(3);
            loadM3uRails();
        } else {
            rowLive.showError("Login não reconhecido. Abra Ajustes e use \"Trocar de lista\".");
            rowMovie.setVisibility(View.GONE);
            rowSeries.setVisibility(View.GONE);
            rail.settings.requestFocus();
        }
    }

    private void beginRefreshState(int pending) {
        railsPending = pending;
        railsOk = false;
        topBar.setUpdating(true);
        rail.setRefreshing(true);
    }

    /** Uma faixa respondeu. Quando as três responderam: guarda a hora e apaga o indicador. */
    private void railDone(boolean ok) {
        if (ok) {
            railsOk = true;
        }
        railsPending--;
        if (railsPending <= 0) {
            railsPending = 0;
            if (railsOk) {
                HomeStore.markUpdated(this, owner);
            }
            topBar.setUpdating(false);
            rail.setRefreshing(false);
            topBar.setUpdatedText(updatedText(HomeStore.lastUpdated(this, owner)));
        }
    }

    private void loadRail(final XtreamApi.Kind kind, final SectionRow row) {
        final String server = creds.server;
        final String user = creds.user;
        final String pass = creds.pass;
        final int gen = railGen;
        new Thread(() -> {
            List<XtreamApi.Item> items = null;
            try {
                items = XtreamApi.items(server, user, pass, kind, null, LIMIT);
            } catch (Exception e) {
                // sem rede, servidor fora do ar ou resposta estranha: mostra aviso abaixo
            }
            if (kind == XtreamApi.Kind.LIVE && items != null) {
                // nomes das categorias (para mostrar no card quando não há guia de programação)
                try {
                    Map<String, String> map = new HashMap<>();
                    for (XtreamApi.Category c : XtreamApi.categories(server, user, pass, kind)) {
                        map.put(c.id, c.name);
                    }
                    catNames = map;
                } catch (Exception e) {
                    // sem nomes: o card mostra "Ao vivo"
                }
            }
            final List<XtreamApi.Item> result = items;
            runOnUiThread(() -> {
                if (gen == railGen) {
                    showRail(kind, result, row);
                }
            });
        }).start();
    }

    /** Lista M3U: uma única leitura da lista traz os 10 primeiros de cada tipo. */
    private void loadM3uRails() {
        final String url = creds.m3uUrl;
        final boolean hideAdult = ParentalStore.isEnabled(this);
        final int gen = railGen;
        new Thread(() -> {
            M3uApi.Sample sample = null;
            try {
                sample = M3uApi.sample(url, LIMIT, hideAdult);
            } catch (Exception e) {
                // sem rede, endereço inválido ou lista estranha: mostra aviso abaixo
            }
            final M3uApi.Sample result = sample;
            runOnUiThread(() -> {
                if (gen != railGen) {
                    return;
                }
                showRail(XtreamApi.Kind.LIVE, result == null ? null : result.live, rowLive);
                showRail(XtreamApi.Kind.MOVIE, result == null ? null : result.movie, rowMovie);
                showRail(XtreamApi.Kind.SERIES, result == null ? null : result.series, rowSeries);
            });
        }).start();
    }

    /** Apaga as faixas e busca tudo de novo (Atualizar, ou quando o controle parental muda). */
    private void reloadRails() {
        if (!isXtream() && !isM3u()) {
            return;
        }
        if (railsPending > 0) {
            return; // já está atualizando
        }
        railGen++;
        heroGen++;
        handler.removeCallbacks(heroBuild);
        userNavigated = true; // não tira o foco de onde a pessoa está
        focusedLive = null;
        detailCache.clear();
        posterByKey.clear();
        liveItems.clear();
        movieItems.clear();
        seriesItems.clear();
        SectionRow[] rows = {rowLive, rowMovie, rowSeries};
        for (SectionRow r : rows) {
            r.rail().removeAllViews();
            r.showLoading();
        }
        loadAllRails();
        loadContinue();
        loadFavorites();
    }

    private void showRail(final XtreamApi.Kind kind, List<XtreamApi.Item> items, SectionRow row) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (items == null) {
            row.showError("Sem conexão. Confira a internet e a lista, e tente Atualizar.");
            railDone(false);
            return;
        }
        // Controle parental ligado: tira o que tem nome adulto
        if (ParentalStore.isEnabled(this)) {
            List<XtreamApi.Item> allowed = new ArrayList<>();
            for (XtreamApi.Item it : items) {
                if (!ParentalStore.isAdultName(it.name)) {
                    allowed.add(it);
                }
            }
            items = allowed;
        }
        if (items.isEmpty()) {
            row.showMessage("Nada encontrado nesta seção.");
            railDone(true);
            storeItems(kind, items);
            afterRail(kind);
            return;
        }
        storeItems(kind, items);

        final LinearLayout railView = row.rail();
        railView.removeAllViews();
        final boolean xtream = isXtream();
        View first = null;
        for (final XtreamApi.Item item : items) {
            View card;
            if (kind == XtreamApi.Kind.LIVE) {
                ChannelCard cc = new ChannelCard(this,
                        track(row, xtream ? item : null));
                cc.bind(item.name, liveSubtitle(item), item.icon);
                if (xtream) {
                    loadNow(item, cc);
                    // OK segurado: segunda via para abrir a programação do canal
                    cc.setOnLongClickListener(v -> {
                        openSchedule(item);
                        return true;
                    });
                }
                railView.addView(cc, ChannelCard.params(this));
                card = cc;
            } else {
                PosterCard pc = new PosterCard(this, track(row, null));
                pc.bind(item.name, kind == XtreamApi.Kind.MOVIE ? "Filme" : "Série", item.icon);
                posterByKey.put(FavoritesStore.identity(item), pc);
                railView.addView(pc, PosterCard.params(this));
                card = pc;
            }
            card.setOnClickListener(v -> openItem(item));
            if (first == null) {
                first = card;
            }
        }
        first.setNextFocusLeftId(navHomeId);

        row.setCount(kind == XtreamApi.Kind.LIVE
                ? items.size() + (items.size() == 1 ? " canal" : " canais")
                : items.size() + " em destaque");
        row.showItems();

        autoFocus(kind == XtreamApi.Kind.LIVE ? RANK_LIVE
                : kind == XtreamApi.Kind.MOVIE ? RANK_MOVIE : RANK_SERIES, first);
        railDone(true);
        afterRail(kind);
    }

    private void storeItems(XtreamApi.Kind kind, List<XtreamApi.Item> items) {
        List<XtreamApi.Item> target = kind == XtreamApi.Kind.LIVE ? liveItems
                : kind == XtreamApi.Kind.MOVIE ? movieItems : seriesItems;
        target.clear();
        target.addAll(items);
    }

    private void afterRail(XtreamApi.Kind kind) {
        if (kind != XtreamApi.Kind.LIVE) {
            // espera um instante para as duas faixas (filmes e séries) chegarem
            handler.removeCallbacks(heroBuild);
            handler.postDelayed(heroBuild, 400L);
        }
    }

    /** Texto de baixo do canal quando não há guia: a categoria (ou "Ao vivo"). */
    private String liveSubtitle(XtreamApi.Item it) {
        String g = it.categoryId == null ? "" : it.categoryId.trim();
        if (isXtream()) {
            String n = catNames.get(g);
            return n != null && !n.trim().isEmpty() ? n.trim() : "Ao vivo";
        }
        return g.isEmpty() ? "Ao vivo" : g;
    }

    /** Pede ao servidor o programa de agora e escreve no card (com a barra de progresso). */
    private void loadNow(final XtreamApi.Item item, final ChannelCard card) {
        EpgCache.get(creds.server, creds.user, creds.pass, item.id, epg -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (epg == null) {
                card.setSubtitle(liveSubtitle(item));
                card.setProgressFraction(-1f);
            } else {
                card.setSubtitle(epg.title);
                card.setProgressFraction(progressOf(epg));
            }
        });
    }

    private static float progressOf(XtreamApi.Epg epg) {
        long span = epg.end - epg.start;
        if (span <= 0) {
            return -1f;
        }
        return (float) (System.currentTimeMillis() - epg.start) / (float) span;
    }

    // ---------------------------------------------------------------------------------
    // Destaque (hero)
    // ---------------------------------------------------------------------------------

    private static boolean hasArt(XtreamApi.Item it) {
        String s = it.icon == null ? "" : it.icon.toLowerCase();
        return s.startsWith("http://") || s.startsWith("https://");
    }

    /**
     * Escolhe o destaque entre os filmes e séries das faixas (com capa). Servidor: pede os
     * detalhes (nota, gênero, ano) de até 8 itens, um de cada vez, e põe o melhor avaliado na
     * frente. Lista M3U: não há nota; usa os primeiros com capa.
     */
    private void buildHero() {
        if (creds == null || isFinishing() || isDestroyed()) {
            return;
        }
        final int gen = ++heroGen;
        final List<XtreamApi.Item> cand = new ArrayList<>();
        int max = Math.max(movieItems.size(), seriesItems.size());
        for (int i = 0; i < max && cand.size() < HERO_CANDIDATES; i++) {
            if (i < movieItems.size() && hasArt(movieItems.get(i))) {
                cand.add(movieItems.get(i));
            }
            if (cand.size() < HERO_CANDIDATES && i < seriesItems.size()
                    && hasArt(seriesItems.get(i))) {
                cand.add(seriesItems.get(i));
            }
        }
        if (cand.isEmpty()) {
            applyHero(gen, new ArrayList<HeroEntry>());
            return;
        }
        final boolean xtream = isXtream();
        final String server = creds.server;
        final String user = creds.user;
        final String pass = creds.pass;
        new Thread(() -> {
            List<HeroEntry> acc = new ArrayList<>();
            for (XtreamApi.Item it : cand) {
                if (gen != heroGen || isFinishing()) {
                    return;
                }
                String key = FavoritesStore.identity(it);
                HeroInfo info = detailCache.get(key);
                if (info == null) {
                    info = new HeroInfo();
                    if (xtream) {
                        try {
                            XtreamApi.Details d = XtreamApi.details(server, user, pass, it.kind, it.id);
                            fillInfo(info, d);
                        } catch (Exception e) {
                            // sem detalhes: o item entra sem nota
                        }
                    }
                    detailCache.put(key, info);
                }
                acc.add(new HeroEntry(it, info));
                final List<HeroEntry> snapshot = topEntries(acc);
                runOnUiThread(() -> applyHero(gen, snapshot));
            }
        }).start();
    }

    /** Ordena por nota (maiores primeiro; sem nota ficam depois, na ordem original). */
    private static List<HeroEntry> topEntries(List<HeroEntry> acc) {
        List<HeroEntry> copy = new ArrayList<>(acc);
        Collections.sort(copy, new Comparator<HeroEntry>() {
            @Override
            public int compare(HeroEntry a, HeroEntry b) {
                return Double.compare(b.info.value, a.info.value);
            }
        });
        if (copy.size() > HERO_MAX) {
            return new ArrayList<>(copy.subList(0, HERO_MAX));
        }
        return copy;
    }

    /** Tira de "Ação • 2021 • 1:45:00 • Nota 7.5" o gênero/ano e a nota. */
    private static void fillInfo(HeroInfo info, XtreamApi.Details d) {
        info.cover = d.cover == null ? "" : d.cover;
        List<String> parts = new ArrayList<>();
        String meta = d.meta == null ? "" : d.meta;
        for (String p : meta.split(" \u2022 ")) {
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.startsWith("Nota ")) {
                info.rating = t.substring(5).trim();
                try {
                    info.value = Double.parseDouble(info.rating.replace(',', '.'));
                } catch (Exception e) {
                    info.value = 0d;
                }
            } else if (!t.matches("\\d+(:\\d+)+") && !t.toLowerCase(Locale.ROOT).endsWith("min")) {
                parts.add(t);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size() && i < 2; i++) {
            if (i > 0) {
                sb.append(" \u00B7 ");
            }
            sb.append(parts.get(i));
        }
        info.metaChip = sb.toString();
    }

    private void applyHero(int gen, List<HeroEntry> list) {
        if (gen != heroGen || isFinishing() || isDestroyed()) {
            return;
        }
        String currentKey = null;
        if (heroIndex >= 0 && heroIndex < heroEntries.size()) {
            currentKey = FavoritesStore.identity(heroEntries.get(heroIndex).item);
        }
        heroEntries = list;

        // nota e gênero/ano também nos pôsteres das faixas
        for (HeroEntry e : list) {
            PosterCard pc = posterByKey.get(FavoritesStore.identity(e.item));
            if (pc != null) {
                pc.setRating(e.info.rating);
                if (!e.info.metaChip.isEmpty()) {
                    pc.setSubtitle(e.info.metaChip);
                }
            }
        }

        if (list.isEmpty()) {
            currentHero = null;
            heroIndex = 0;
            hero.showLoading();
            return;
        }
        int idx = 0;
        if (currentKey != null) {
            for (int i = 0; i < list.size(); i++) {
                if (FavoritesStore.identity(list.get(i).item).equals(currentKey)) {
                    idx = i;
                    break;
                }
            }
        }
        showHero(idx);
    }

    /** Mostra o destaque "index". Nunca pede foco (só o foco inicial, uma vez). */
    private void showHero(int index) {
        if (index < 0 || index >= heroEntries.size()) {
            return;
        }
        HeroEntry e = heroEntries.get(index);
        heroIndex = index;
        currentHero = e.item;
        hero.showEntry(e.item.name,
                e.item.kind == XtreamApi.Kind.MOVIE ? "Filme" : "Série",
                e.info.metaChip, e.info.rating,
                e.info.cover.isEmpty() ? e.item.icon : e.info.cover,
                favKeys.contains(FavoritesStore.identity(e.item)));
        autoFocus(RANK_HERO, hero.playButton());
    }

    private void toggleHeroFavorite() {
        final XtreamApi.Item it = currentHero;
        if (it == null) {
            return;
        }
        new Thread(() -> {
            final boolean nowIn = FavoritesStore.toggle(this, owner, it);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                String k = FavoritesStore.identity(it);
                if (nowIn) {
                    favKeys.add(k);
                } else {
                    favKeys.remove(k);
                }
                if (currentHero != null && FavoritesStore.identity(currentHero).equals(k)) {
                    hero.setInList(nowIn);
                }
                Toast.makeText(this, nowIn ? "Adicionado à Minha lista" : "Removido da Minha lista",
                        Toast.LENGTH_SHORT).show();
                loadFavorites();
            });
        }).start();
    }

    // ---------------------------------------------------------------------------------
    // Faixa "Continue assistindo"
    // ---------------------------------------------------------------------------------

    /** Lê a ContinueStore fora da thread principal e monta a faixa. */
    private void loadContinue() {
        if (creds == null) {
            return;
        }
        final int gen = ++continueGen;
        new Thread(() -> {
            List<ContinueStore.Saved> all = null;
            try {
                all = ContinueStore.list(this);
            } catch (Exception e) {
                // não conseguiu ler: a faixa fica escondida
            }
            final List<ContinueStore.Saved> result = all;
            runOnUiThread(() -> {
                if (gen == continueGen) {
                    showContinue(result);
                }
            });
        }).start();
    }

    private void showContinue(List<ContinueStore.Saved> list) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        boolean hadFocus = rowContinue.rail().hasFocus();
        rowContinue.rail().removeAllViews();

        // Controle parental ligado: tira o que tem nome adulto. No máximo 10 na faixa.
        boolean hideAdult = ParentalStore.isEnabled(this);
        List<ContinueStore.Saved> shown = new ArrayList<>();
        int total = 0;
        if (list != null) {
            for (ContinueStore.Saved s : list) {
                if (s.url.isEmpty()) {
                    continue;
                }
                if (hideAdult && ParentalStore.isAdultName(s.name)) {
                    continue;
                }
                total++;
                if (shown.size() < LIMIT) {
                    shown.add(s);
                }
            }
        }

        if (shown.isEmpty()) {
            rowContinue.setVisibility(View.GONE);
            if (hadFocus) {
                hero.playButton().requestFocus();
            }
            return;
        }

        rowContinue.setVisibility(View.VISIBLE);
        View first = null;
        for (final ContinueStore.Saved s : shown) {
            PosterCard card = new PosterCard(this, track(rowContinue, null));
            card.bind(s.name, "Parou em " + clock(s.posMs), s.icon);
            if (s.durMs > 0 && s.posMs > 0) {
                card.setProgressFraction(Math.max(0f, Math.min(1f, (float) s.posMs / (float) s.durMs)));
            }
            card.setOnClickListener(v -> openContinue(s));
            rowContinue.rail().addView(card, PosterCard.params(this));
            if (first == null) {
                first = card;
            }
        }
        first.setNextFocusLeftId(navHomeId);
        rowContinue.setCount(total + (total == 1 ? " item" : " itens"));
        rowContinue.showItems();

        if (hadFocus) {
            first.requestFocus();
        } else {
            autoFocus(RANK_CONTINUE, first);
        }
    }

    /**
     * Cartão do "Continue assistindo": filme abre a página do filme; episódio abre a página
     * da série. Em ambas, a pessoa clica em "Continuar".
     */
    private void openContinue(ContinueStore.Saved s) {
        if ("movie".equals(s.kind)) {
            Intent i = new Intent(this, MovieActivity.class);
            i.putExtra(EXTRA_KIND, XtreamApi.Kind.MOVIE.name());
            i.putExtra(EXTRA_ID, s.id);
            i.putExtra(EXTRA_EXT, s.ext);
            i.putExtra(EXTRA_NAME, s.name);
            if (!s.icon.isEmpty()) {
                i.putExtra(MovieActivity.EXTRA_ICON, s.icon);
            }
            if (isM3u()) {
                i.putExtra(MainActivity.EXTRA_URL, s.url);
            }
            startActivity(i);
            return;
        }

        if ("episode".equals(s.kind)) {
            Intent i;
            if (isXtream() && s.seriesKey.startsWith("xt|")) {
                // Chave: xt|servidor|usuário|número da série. O número é a última parte.
                int p = s.seriesKey.lastIndexOf('|');
                String seriesId = p >= 0 ? s.seriesKey.substring(p + 1) : "";
                int q = s.name.lastIndexOf(" - ep. ");
                String seriesName = q > 0 ? s.name.substring(0, q).trim() : s.name;
                if (!seriesId.matches("\\d{1,12}")) {
                    cannotOpen();
                    return;
                }
                i = new Intent(this, SeriesActivity.class);
                i.putExtra(EXTRA_ID, seriesId);
                i.putExtra(EXTRA_NAME, seriesName);
            } else if (isM3u() && s.seriesKey.startsWith("m3u|")) {
                // Lista M3U: a série é achada pelo título, que vem antes de " - ep. N"
                int q = s.name.lastIndexOf(" - ep. ");
                String seriesName = q > 0 ? s.name.substring(0, q).trim() : "";
                if (seriesName.isEmpty()) {
                    cannotOpen();
                    return;
                }
                i = new Intent(this, M3uSeriesActivity.class);
                i.putExtra(EXTRA_NAME, seriesName);
            } else {
                cannotOpen();
                return;
            }
            if (!s.icon.isEmpty()) {
                i.putExtra(MovieActivity.EXTRA_ICON, s.icon);
            }
            startActivity(i);
            return;
        }

        cannotOpen();
    }

    private void cannotOpen() {
        Toast.makeText(this, "Não deu para abrir este item. Ele pode ser de outra lista.",
                Toast.LENGTH_LONG).show();
    }

    /** Minuto em texto: 5:07 ou 1:23:45. */
    private static String clock(long ms) {
        long total = ms / 1000L;
        long h = total / 3600L;
        long m = (total % 3600L) / 60L;
        long sec = total % 60L;
        if (h > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", h, m, sec);
        }
        return String.format(Locale.US, "%d:%02d", m, sec);
    }

    // ---------------------------------------------------------------------------------
    // Faixa "Minha lista"
    // ---------------------------------------------------------------------------------

    private void loadFavorites() {
        if (creds == null) {
            return;
        }
        final int gen = ++favGen;
        new Thread(() -> {
            final List<XtreamApi.Item> all = FavoritesStore.list(this, owner);
            runOnUiThread(() -> {
                if (gen == favGen) {
                    showFavorites(all);
                }
            });
        }).start();
    }

    private void showFavorites(List<XtreamApi.Item> all) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        boolean hadFocus = rowFav.rail().hasFocus();
        rowFav.rail().removeAllViews();

        boolean hideAdult = ParentalStore.isEnabled(this);
        List<XtreamApi.Item> shown = new ArrayList<>();
        favKeys.clear();
        for (XtreamApi.Item it : all) {
            favKeys.add(FavoritesStore.identity(it));
            if (hideAdult && ParentalStore.isAdultName(it.name)) {
                continue;
            }
            shown.add(it);
        }
        // o botão do destaque acompanha a lista
        if (currentHero != null) {
            hero.setInList(favKeys.contains(FavoritesStore.identity(currentHero)));
        }

        if (shown.isEmpty()) {
            rowFav.setVisibility(View.GONE);
            if (hadFocus) {
                hero.listButton().requestFocus();
            }
            return;
        }

        rowFav.setVisibility(View.VISIBLE);
        View first = null;
        for (final XtreamApi.Item item : shown) {
            PosterCard card = new PosterCard(this, track(rowFav, null));
            card.bind(item.name, item.kind == XtreamApi.Kind.MOVIE ? "Filme" : "Série", item.icon);
            card.setOnClickListener(v -> openItem(item));
            // OK segurado: tira da lista
            card.setOnLongClickListener(v -> {
                removeFavorite(item);
                return true;
            });
            rowFav.rail().addView(card, PosterCard.params(this));
            if (first == null) {
                first = card;
            }
        }
        first.setNextFocusLeftId(navHomeId);
        rowFav.setCount(shown.size() + " na lista \u00B7 segure OK para tirar");
        rowFav.showItems();

        if (hadFocus) {
            first.requestFocus();
        } else {
            autoFocus(RANK_FAV, first);
        }
    }

    private void removeFavorite(final XtreamApi.Item item) {
        new Thread(() -> {
            if (FavoritesStore.contains(this, owner, item)) {
                FavoritesStore.toggle(this, owner, item);
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Toast.makeText(this, "Removido da Minha lista", Toast.LENGTH_SHORT).show();
                loadFavorites();
            });
        }).start();
    }

    // ---------------------------------------------------------------------------------
    // Abrir telas
    // ---------------------------------------------------------------------------------

    /** Abre a janela da programação de um canal (só login por servidor). */
    private void openSchedule(XtreamApi.Item item) {
        if (!isXtream() || item == null || item.kind != XtreamApi.Kind.LIVE) {
            return;
        }
        EpgDialog.show(this, creds.server, creds.user, creds.pass, item.id, item.name);
    }

    private void openSearch() {
        if (!isXtream() && !isM3u()) {
            return;
        }
        SearchKeyboard.show(this, "Buscar canais, filmes e séries", new SearchKeyboard.Listener() {
            @Override
            public void onSearch(String text) {
                Intent i = new Intent(HomeActivity.this, SearchAllActivity.class);
                i.putExtra(SearchActivity.EXTRA_QUERY, text);
                startActivity(i);
            }

            @Override
            public void onCancel() {
                // nada a fazer
            }
        });
    }

    /** Canais, Filmes e Séries: abrem a aba daquele tipo. */
    private void openSection(XtreamApi.Kind kind) {
        if (!isXtream() && !isM3u()) {
            return;
        }
        openBrowse(kind);
    }

    /** Abre a aba: a tela do servidor ou a da lista M3U, conforme o login. */
    private void openBrowse(XtreamApi.Kind kind) {
        if (!isXtream() && !isM3u()) {
            return;
        }
        Intent i;
        if (isM3u()) {
            i = new Intent(this, M3uBrowseActivity.class);
        } else {
            i = new Intent(this, BrowseActivity.class);
        }
        i.putExtra(EXTRA_KIND, kind.name());
        startActivity(i);
    }

    private void openItem(final XtreamApi.Item item) {
        if (item == null) {
            return;
        }
        if (item.kind == XtreamApi.Kind.SERIES) {
            // Lista M3U: a série é achada pelo título; servidor: pelo número
            Intent s = new Intent(this, isM3u() ? M3uSeriesActivity.class : SeriesActivity.class);
            s.putExtra(EXTRA_ID, item.id);
            s.putExtra(EXTRA_NAME, item.name);
            s.putExtra(MovieActivity.EXTRA_ICON, item.icon);
            startActivity(s);
            return;
        }
        // Filme: abre a página do filme (capa e sinopse). Canal ao vivo: toca direto.
        Intent i = new Intent(this,
                item.kind == XtreamApi.Kind.MOVIE ? MovieActivity.class : MainActivity.class);
        i.putExtra(EXTRA_KIND, item.kind.name());
        i.putExtra(EXTRA_ID, item.id);
        i.putExtra(EXTRA_EXT, item.ext);
        i.putExtra(EXTRA_NAME, item.name);
        if (item.kind == XtreamApi.Kind.MOVIE) {
            i.putExtra(MovieActivity.EXTRA_ICON, item.icon);
        }
        if (item.kind == XtreamApi.Kind.LIVE) {
            // R4: o player troca de canal com cima e baixo. Categoria não enviada = todas.
            i.putExtra(MainActivity.EXTRA_QUEUE, true);
            // lembra o canal para o card "AO VIVO AGORA"
            final String ownerNow = owner;
            new Thread(() -> HomeStore.saveLastLive(getApplicationContext(), ownerNow, item)).start();
        }
        if (isM3u()) {
            i.putExtra(MainActivity.EXTRA_URL, item.url);
        }
        startActivity(i);
    }

    // ---------------------------------------------------------------------------------
    // Chip "Lista atualizada"
    // ---------------------------------------------------------------------------------

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static String updatedText(long ms) {
        if (ms <= 0L) {
            return "Lista ainda não atualizada";
        }
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(ms);
        String time = String.format(Locale.US, "%02d:%02d",
                then.get(Calendar.HOUR_OF_DAY), then.get(Calendar.MINUTE));
        Calendar today = Calendar.getInstance();
        if (sameDay(today, then)) {
            return "Lista atualizada hoje, " + time;
        }
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(yesterday, then)) {
            return "Lista atualizada ontem, " + time;
        }
        return String.format(Locale.US, "Lista atualizada %02d/%02d, %s",
                then.get(Calendar.DAY_OF_MONTH), then.get(Calendar.MONTH) + 1, time);
    }

    // ---------------------------------------------------------------------------------
    // Atualização automática do app
    // ---------------------------------------------------------------------------------

    private void checkForUpdate() {
        Updater.checkAsync(this, new Updater.CheckListener() {
            @Override
            public void onUpdateAvailable(Updater.Info info) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                pendingUpdate = info;
                tvUpdate.setTextColor(GlassKit.color(HomeActivity.this, R.color.asc_focus));
                tvUpdate.setText(getString(R.string.update_available, info.name));
                updateBar.setVisibility(View.VISIBLE);
                btnUpdateNow.requestFocus();
            }

            @Override
            public void onNoUpdate() {
                // nada a fazer
            }
        });
    }

    private void startUpdate() {
        if (downloading || pendingUpdate == null) {
            return;
        }
        downloading = true;
        btnUpdateNow.setEnabled(false);
        btnUpdateLater.setEnabled(false);
        tvUpdate.setTextColor(GlassKit.color(this, R.color.asc_text_primary));
        tvUpdate.setText(R.string.msg_downloading_unknown);

        Updater.downloadAndInstallAsync(this, pendingUpdate, new Updater.InstallListener() {
            @Override
            public void onProgress(int percent) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (percent >= 0) {
                    tvUpdate.setText(getString(R.string.msg_downloading, percent));
                } else {
                    tvUpdate.setText(R.string.msg_downloading_unknown);
                }
            }

            @Override
            public void onInstallerOpened() {
                downloading = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                tvUpdate.setText(R.string.msg_installer_opened);
                btnUpdateNow.setEnabled(true);
                btnUpdateLater.setEnabled(true);
            }

            @Override
            public void onFailed(String message) {
                downloading = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                tvUpdate.setTextColor(GlassKit.color(HomeActivity.this, R.color.asc_text_primary));
                tvUpdate.setText(message);
                btnUpdateNow.setEnabled(true);
                btnUpdateLater.setEnabled(true);
                btnUpdateNow.requestFocus();
            }
        });
    }

    private void dismissUpdate() {
        if (downloading) {
            return;
        }
        updateBar.setVisibility(View.GONE);
        hero.playButton().requestFocus();
    }
}

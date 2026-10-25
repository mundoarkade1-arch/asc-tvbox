package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Página inicial: 10 primeiros canais, filmes e séries.
 * Servidor com login (Xtream): busca só o começo de cada lista (nunca a lista inteira).
 * No fim de cada faixa há o cartão "Ver mais ›", que abre a navegação por categorias.
 * Nos canais ao vivo, cada cartão mostra o programa que passa agora (EPG por canal).
 * Não há caixinha de prévia nesta tela (pedido do dono, Rodada 1, R2).
 * Sobre um canal ao vivo (servidor), a tecla das três listras ou segurar o OK abre a janela
 * com a programação do canal (o que passa agora e os próximos programas).
 * Ao escolher uma série, abre a tela da série (temporadas e episódios), levando a capa do item.
 * Ao escolher um filme, abre a página do filme (MovieActivity), com capa e sinopse.
 *
 * Lista M3U: lê a lista aos poucos, só até achar 10 itens de cada tipo (M3uApi).
 * O cartão "Ver mais ›" abre a navegação por categorias da lista M3U (M3uBrowseActivity).
 * Ao escolher uma série, abre a tela da série da lista M3U (M3uSeriesActivity).
 * Por enquanto, sem guia de programação (chega em etapa própria).
 *
 * Com o controle parental ligado, itens com nome adulto não aparecem nas faixas. O controle
 * parental e a troca de lista ficam na aba Ajustes (SettingsActivity). Ao voltar de lá, esta
 * tela confere se o controle parental mudou e, se mudou, recarrega as faixas.
 *
 * A busca por nome NÃO fica aqui: cada aba (Canais, Filmes, Séries) tem o seu
 * botão "Pesquisar ..." (BrowseActivity e M3uBrowseActivity).
 *
 * Menu à esquerda (ícone e nome): Tela inicial, Canais, Filmes, Séries, Ajustes e Atualizar.
 * Canais, Filmes e Séries abrem a aba; Ajustes abre a SettingsActivity.
 */
public class HomeActivity extends Activity {

    public static final String EXTRA_KIND = "kind";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_EXT = "ext";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_SHOW_LOGIN = "show_login";

    private static final int LIMIT = 10;
    private static final int COLOR_NEUTRAL = 0xFFA39A9D;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    private static final int COLOR_OK = 0xFF4ADE80;
    private static final int COLOR_WHITE = 0xFFFFFFFF;

    private CredentialStore.Credentials creds;

    private LinearLayout railLive;
    private LinearLayout railMovie;
    private LinearLayout railSeries;
    private TextView stLive;
    private TextView stMovie;
    private TextView stSeries;

    // Menu à esquerda (ícone e nome)
    private View navHome;
    private View navLive;
    private View navMovie;
    private View navSeries;
    private View navSettings;
    private View navRefresh;

    // Para ignorar respostas antigas quando as faixas são recarregadas
    private int railGen = 0;

    // Estado do controle parental na última vez que as faixas foram montadas
    private boolean parentalSeen = false;

    private View groupUpdate;
    private TextView tvUpdate;
    private Button btnUpdateNow;
    private Button btnUpdateLater;
    private Updater.Info pendingUpdate;
    private boolean downloading = false;

    // Canal ao vivo (servidor) que está com o foco agora; vazio quando o foco está em outro lugar.
    // Serve só para a tecla de menu abrir a programação do canal.
    private XtreamApi.Item focusedLive;

    private final View.OnFocusChangeListener clearLiveOnFocus = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                focusedLive = null;
            }
        }
    };

    private boolean focusDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        creds = CredentialStore.load(this);
        if (creds == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        parentalSeen = ParentalStore.isEnabled(this);

        setContentView(R.layout.activity_home);

        railLive = findViewById(R.id.rail_live);
        railMovie = findViewById(R.id.rail_movie);
        railSeries = findViewById(R.id.rail_series);
        stLive = findViewById(R.id.st_live);
        stMovie = findViewById(R.id.st_movie);
        stSeries = findViewById(R.id.st_series);

        navHome = findViewById(R.id.nav_home);
        navLive = findViewById(R.id.nav_live);
        navMovie = findViewById(R.id.nav_movie);
        navSeries = findViewById(R.id.nav_series);
        navSettings = findViewById(R.id.nav_settings);
        navRefresh = findViewById(R.id.nav_refresh);

        groupUpdate = findViewById(R.id.group_update);
        tvUpdate = findViewById(R.id.tv_update);
        btnUpdateNow = findViewById(R.id.btn_update_now);
        btnUpdateLater = findViewById(R.id.btn_update_later);

        btnUpdateNow.setOnFocusChangeListener(clearLiveOnFocus);
        btnUpdateLater.setOnFocusChangeListener(clearLiveOnFocus);

        navHome.setOnFocusChangeListener(clearLiveOnFocus);
        navLive.setOnFocusChangeListener(clearLiveOnFocus);
        navMovie.setOnFocusChangeListener(clearLiveOnFocus);
        navSeries.setOnFocusChangeListener(clearLiveOnFocus);
        navSettings.setOnFocusChangeListener(clearLiveOnFocus);
        navRefresh.setOnFocusChangeListener(clearLiveOnFocus);

        btnUpdateNow.setOnClickListener(v -> startUpdate());
        btnUpdateLater.setOnClickListener(v -> dismissUpdate());

        navHome.setOnClickListener(v -> goToFirstCard());
        navLive.setOnClickListener(v -> openSection(XtreamApi.Kind.LIVE));
        navMovie.setOnClickListener(v -> openSection(XtreamApi.Kind.MOVIE));
        navSeries.setOnClickListener(v -> openSection(XtreamApi.Kind.SERIES));
        navSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        navRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Atualizando a lista...", Toast.LENGTH_SHORT).show();
            reloadRails();
        });

        loadAllRails();

        checkForUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Voltou de outra tela (por exemplo, Ajustes): se o controle parental mudou, recarrega
        if (creds != null && ParentalStore.isEnabled(this) != parentalSeen) {
            parentalSeen = ParentalStore.isEnabled(this);
            reloadRails();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && isXtream() && focusedLive != null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                openSchedule(focusedLive);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /** Abre a janela da programação de um canal (só login por servidor). */
    private void openSchedule(XtreamApi.Item item) {
        if (!isXtream() || item == null || item.kind != XtreamApi.Kind.LIVE) {
            return;
        }
        EpgDialog.show(this, creds.server, creds.user, creds.pass, item.id, item.name);
    }

    // ---------- Menu à esquerda ----------

    /** Tela inicial: leva o foco para o primeiro canal (ou filme, ou série) já carregado. */
    private void goToFirstCard() {
        LinearLayout[] rails = {railLive, railMovie, railSeries};
        for (LinearLayout rail : rails) {
            if (rail.getChildCount() > 0) {
                rail.getChildAt(0).requestFocus();
                return;
            }
        }
    }

    /** Canais, Filmes e Séries: abrem a aba daquele tipo. */
    private void openSection(XtreamApi.Kind kind) {
        if (!isXtream() && !isM3u()) {
            return;
        }
        openBrowse(kind);
    }

    // ---------- Faixas de canais, filmes e séries ----------

    private boolean isXtream() {
        return creds != null && CredentialStore.MODE_XTREAM.equals(creds.mode);
    }

    private boolean isM3u() {
        return creds != null && CredentialStore.MODE_M3U.equals(creds.mode);
    }

    /** Busca as três faixas, de acordo com o tipo de login. */
    private void loadAllRails() {
        if (isXtream()) {
            loadRail(XtreamApi.Kind.LIVE, railLive, stLive);
            loadRail(XtreamApi.Kind.MOVIE, railMovie, stMovie);
            loadRail(XtreamApi.Kind.SERIES, railSeries, stSeries);
        } else if (isM3u()) {
            loadM3uRails();
        } else {
            stLive.setTextColor(COLOR_ERROR);
            stLive.setText("Login não reconhecido. Abra Ajustes e use \"Trocar de lista\".");
            stMovie.setVisibility(View.GONE);
            stSeries.setVisibility(View.GONE);
            navSettings.requestFocus();
        }
    }

    private void loadRail(final XtreamApi.Kind kind, final LinearLayout rail, final TextView status) {
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
            final List<XtreamApi.Item> result = items;
            runOnUiThread(() -> {
                if (gen == railGen) {
                    showRail(kind, result, rail, status);
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
                showRail(XtreamApi.Kind.LIVE, result == null ? null : result.live,
                        railLive, stLive);
                showRail(XtreamApi.Kind.MOVIE, result == null ? null : result.movie,
                        railMovie, stMovie);
                showRail(XtreamApi.Kind.SERIES, result == null ? null : result.series,
                        railSeries, stSeries);
            });
        }).start();
    }

    /** Apaga as faixas e busca tudo de novo (Atualizar, ou quando o controle parental muda). */
    private void reloadRails() {
        if (!isXtream() && !isM3u()) {
            return;
        }
        railGen++;
        focusDone = true; // não tira o foco de onde a pessoa está
        focusedLive = null;
        LinearLayout[] rails = {railLive, railMovie, railSeries};
        TextView[] states = {stLive, stMovie, stSeries};
        for (int i = 0; i < rails.length; i++) {
            rails[i].removeAllViews();
            states[i].setTextColor(COLOR_NEUTRAL);
            states[i].setText("Carregando...");
            states[i].setVisibility(View.VISIBLE);
        }
        loadAllRails();
    }

    private void showRail(final XtreamApi.Kind kind, List<XtreamApi.Item> items,
                          LinearLayout rail, TextView status) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (items == null) {
            status.setTextColor(COLOR_ERROR);
            status.setText("Não foi possível carregar. Confira a internet e o servidor.");
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
            status.setTextColor(COLOR_NEUTRAL);
            status.setText("Nada encontrado nesta seção.");
            return;
        }
        status.setVisibility(View.GONE);
        final boolean xtream = isXtream();
        LayoutInflater inflater = LayoutInflater.from(this);
        View first = null;
        for (final XtreamApi.Item item : items) {
            View card = inflater.inflate(R.layout.item_card, rail, false);
            TextView initial = card.findViewById(R.id.tv_initial);
            TextView name = card.findViewById(R.id.tv_name);
            ImageView logo = card.findViewById(R.id.iv_logo);

            name.setText(item.name);
            initial.setText(firstLetter(item.name));
            ImageLoader.load(logo, item.icon);

            if (item.kind == XtreamApi.Kind.LIVE && xtream) {
                TextView now = card.findViewById(R.id.tv_now);
                now.setText("");
                now.setVisibility(View.VISIBLE);
                loadNow(item, now);
                card.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        focusedLive = item;
                    }
                });
                // OK segurado: segunda via para abrir a programação do canal
                card.setOnLongClickListener(v -> {
                    openSchedule(item);
                    return true;
                });
            } else {
                if (item.kind == XtreamApi.Kind.LIVE) {
                    // Lista M3U: ainda sem guia de programação
                    TextView now = card.findViewById(R.id.tv_now);
                    now.setText("");
                    now.setVisibility(View.GONE);
                }
                card.setOnFocusChangeListener(clearLiveOnFocus);
            }

            card.setOnClickListener(v -> openItem(item));
            rail.addView(card);
            if (first == null) {
                first = card;
            }
        }

        // Cartão "Ver mais ›" no fim da faixa (login por servidor e por lista M3U)
        if (xtream || isM3u()) {
            View more = inflater.inflate(R.layout.item_card, rail, false);
            TextView moreInitial = more.findViewById(R.id.tv_initial);
            TextView moreName = more.findViewById(R.id.tv_name);
            moreName.setText("Ver mais ›");
            moreInitial.setText("›");
            more.setOnFocusChangeListener(clearLiveOnFocus);
            more.setOnClickListener(v -> openBrowse(kind));
            rail.addView(more);
        }

        if (!focusDone && first != null && groupUpdate.getVisibility() != View.VISIBLE) {
            focusDone = true;
            first.requestFocus();
        }
    }

    /** Pede ao servidor o programa de agora e escreve na linha do cartão. */
    private void loadNow(XtreamApi.Item item, final TextView now) {
        EpgCache.get(creds.server, creds.user, creds.pass, item.id, epg -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (epg == null) {
                now.setText("Programação indisponível");
            } else {
                now.setText("Agora: " + epg.title);
            }
        });
    }

    private static String firstLetter(String name) {
        String s = name == null ? "" : name.trim();
        if (s.isEmpty()) {
            return "?";
        }
        return new String(Character.toChars(s.codePointAt(0))).toUpperCase();
    }

    /** Abre a aba: a tela do servidor ou a da lista M3U, conforme o login. */
    private void openBrowse(XtreamApi.Kind kind) {
        Intent i;
        if (isM3u()) {
            i = new Intent(this, M3uBrowseActivity.class);
        } else {
            i = new Intent(this, BrowseActivity.class);
        }
        i.putExtra(EXTRA_KIND, kind.name());
        startActivity(i);
    }

    private void openItem(XtreamApi.Item item) {
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
        if (isM3u()) {
            i.putExtra(MainActivity.EXTRA_URL, item.url);
        }
        startActivity(i);
    }

    // ---------- Atualização automática ----------

    private void checkForUpdate() {
        Updater.checkAsync(this, new Updater.CheckListener() {
            @Override
            public void onUpdateAvailable(Updater.Info info) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                pendingUpdate = info;
                tvUpdate.setTextColor(COLOR_OK);
                tvUpdate.setText(getString(R.string.update_available, info.name));
                groupUpdate.setVisibility(View.VISIBLE);
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
        tvUpdate.setTextColor(COLOR_WHITE);
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
                tvUpdate.setTextColor(COLOR_ERROR);
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
        groupUpdate.setVisibility(View.GONE);
        navHome.requestFocus();
    }
}

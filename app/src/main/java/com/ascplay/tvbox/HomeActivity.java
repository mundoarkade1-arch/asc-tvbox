package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Página inicial: 10 primeiros canais, filmes e séries.
 * Para servidor com login (Xtream), busca só o começo de cada lista (nunca a lista inteira).
 * No fim de cada faixa há o cartão "Ver mais ›", que abre a navegação por categorias.
 * Nos canais ao vivo, cada cartão mostra o programa que passa agora (EPG por canal).
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

    private Button btnLogout;
    private LinearLayout railLive;
    private LinearLayout railMovie;
    private LinearLayout railSeries;
    private TextView stLive;
    private TextView stMovie;
    private TextView stSeries;

    private View groupUpdate;
    private TextView tvUpdate;
    private Button btnUpdateNow;
    private Button btnUpdateLater;
    private Updater.Info pendingUpdate;
    private boolean downloading = false;

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

        setContentView(R.layout.activity_home);

        btnLogout = findViewById(R.id.btn_logout);
        railLive = findViewById(R.id.rail_live);
        railMovie = findViewById(R.id.rail_movie);
        railSeries = findViewById(R.id.rail_series);
        stLive = findViewById(R.id.st_live);
        stMovie = findViewById(R.id.st_movie);
        stSeries = findViewById(R.id.st_series);

        groupUpdate = findViewById(R.id.group_update);
        tvUpdate = findViewById(R.id.tv_update);
        btnUpdateNow = findViewById(R.id.btn_update_now);
        btnUpdateLater = findViewById(R.id.btn_update_later);

        btnLogout.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra(EXTRA_SHOW_LOGIN, true);
            startActivity(i);
            finish();
        });
        btnUpdateNow.setOnClickListener(v -> startUpdate());
        btnUpdateLater.setOnClickListener(v -> dismissUpdate());

        if (CredentialStore.MODE_XTREAM.equals(creds.mode)) {
            loadRail(XtreamApi.Kind.LIVE, railLive, stLive);
            loadRail(XtreamApi.Kind.MOVIE, railMovie, stMovie);
            loadRail(XtreamApi.Kind.SERIES, railSeries, stSeries);
        } else {
            String msg = "Lista M3U: a página inicial para este tipo de login chega numa etapa própria.";
            stLive.setText(msg);
            stMovie.setText("");
            stSeries.setText("");
            stMovie.setVisibility(View.GONE);
            stSeries.setVisibility(View.GONE);
            btnLogout.requestFocus();
        }

        checkForUpdate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clear();
        EpgCache.clear();
    }

    // ---------- Faixas de canais, filmes e séries ----------

    private void loadRail(final XtreamApi.Kind kind, final LinearLayout rail, final TextView status) {
        final String server = creds.server;
        final String user = creds.user;
        final String pass = creds.pass;
        new Thread(() -> {
            List<XtreamApi.Item> items = null;
            try {
                items = XtreamApi.items(server, user, pass, kind, null, LIMIT);
            } catch (Exception e) {
                // sem rede, servidor fora do ar ou resposta estranha: mostra aviso abaixo
            }
            final List<XtreamApi.Item> result = items;
            runOnUiThread(() -> showRail(kind, result, rail, status));
        }).start();
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
        if (items.isEmpty()) {
            status.setTextColor(COLOR_NEUTRAL);
            status.setText("Nada encontrado nesta seção.");
            return;
        }
        status.setVisibility(View.GONE);
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

            if (item.kind == XtreamApi.Kind.LIVE) {
                TextView now = card.findViewById(R.id.tv_now);
                now.setText("");
                now.setVisibility(View.VISIBLE);
                loadNow(item, now);
            }

            card.setOnClickListener(v -> openItem(item));
            rail.addView(card);
            if (first == null) {
                first = card;
            }
        }

        // Cartão "Ver mais ›" no fim da faixa
        View more = inflater.inflate(R.layout.item_card, rail, false);
        TextView moreInitial = more.findViewById(R.id.tv_initial);
        TextView moreName = more.findViewById(R.id.tv_name);
        moreName.setText("Ver mais ›");
        moreInitial.setText("›");
        more.setOnClickListener(v -> openBrowse(kind));
        rail.addView(more);

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

    private void openBrowse(XtreamApi.Kind kind) {
        Intent i = new Intent(this, BrowseActivity.class);
        i.putExtra(EXTRA_KIND, kind.name());
        startActivity(i);
    }

    private void openItem(XtreamApi.Item item) {
        if (item.kind == XtreamApi.Kind.SERIES) {
            Toast.makeText(this, "Séries: os episódios chegam na próxima etapa.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(EXTRA_KIND, item.kind.name());
        i.putExtra(EXTRA_ID, item.id);
        i.putExtra(EXTRA_EXT, item.ext);
        i.putExtra(EXTRA_NAME, item.name);
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
        btnLogout.requestFocus();
    }
}

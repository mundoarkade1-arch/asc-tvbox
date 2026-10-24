package com.ascplay.tvbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Página inicial: 10 primeiros canais, filmes e séries.
 * Para servidor com login (Xtream), busca só o começo de cada lista (nunca a lista inteira).
 * No fim de cada faixa há o cartão "Ver mais ›", que abre a navegação por categorias.
 * Nos canais ao vivo, cada cartão mostra o programa que passa agora (EPG por canal).
 * Ao parar o foco num canal ao vivo, uma caixinha fixa embaixo mostra a prévia:
 * programa de agora, barra de progresso, horários e "A seguir".
 * O botão "Controle parental" ativa, troca ou desativa a senha de 4 números.
 * Com o controle ligado, itens com nome adulto não aparecem nas faixas.
 * Ao escolher uma série, abre a tela da série (temporadas e episódios).
 */
public class HomeActivity extends Activity {

    public static final String EXTRA_KIND = "kind";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_EXT = "ext";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_SHOW_LOGIN = "show_login";

    private static final int LIMIT = 10;
    private static final long PREVIEW_DELAY_MS = 500L;
    private static final int COLOR_NEUTRAL = 0xFFA39A9D;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    private static final int COLOR_OK = 0xFF4ADE80;
    private static final int COLOR_WHITE = 0xFFFFFFFF;

    private CredentialStore.Credentials creds;

    private Button btnLogout;
    private Button btnParental;
    private LinearLayout railLive;
    private LinearLayout railMovie;
    private LinearLayout railSeries;
    private TextView stLive;
    private TextView stMovie;
    private TextView stSeries;

    // Para ignorar respostas antigas quando as faixas são recarregadas
    private int railGen = 0;

    private View groupUpdate;
    private TextView tvUpdate;
    private Button btnUpdateNow;
    private Button btnUpdateLater;
    private Updater.Info pendingUpdate;
    private boolean downloading = false;

    // Controle parental: teclado aberto no momento (para fechar ao sair da tela)
    private PinDialog pinDialog;

    // Caixinha de prévia do canal
    private View groupPreview;
    private TextView tvPvChannel;
    private TextView tvPvTitle;
    private TextView tvPvStart;
    private TextView tvPvEnd;
    private TextView tvPvNext;
    private ProgressBar pbPreview;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private XtreamApi.Item previewItem;
    private final Runnable previewRunnable = new Runnable() {
        @Override
        public void run() {
            if (previewItem != null) {
                fetchPreview(previewItem);
            }
        }
    };
    private final View.OnFocusChangeListener hideOnFocus = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                hidePreview();
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

        setContentView(R.layout.activity_home);

        btnLogout = findViewById(R.id.btn_logout);
        btnParental = findViewById(R.id.btn_parental);
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

        groupPreview = findViewById(R.id.group_preview);
        tvPvChannel = findViewById(R.id.tv_pv_channel);
        tvPvTitle = findViewById(R.id.tv_pv_title);
        tvPvStart = findViewById(R.id.tv_pv_start);
        tvPvEnd = findViewById(R.id.tv_pv_end);
        tvPvNext = findViewById(R.id.tv_pv_next);
        pbPreview = findViewById(R.id.pb_preview);

        btnLogout.setOnFocusChangeListener(hideOnFocus);
        btnParental.setOnFocusChangeListener(hideOnFocus);
        btnUpdateNow.setOnFocusChangeListener(hideOnFocus);
        btnUpdateLater.setOnFocusChangeListener(hideOnFocus);

        btnLogout.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra(EXTRA_SHOW_LOGIN, true);
            startActivity(i);
            finish();
        });
        btnParental.setOnClickListener(v -> openParentalMenu());
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
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(previewRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(previewRunnable);
        previewItem = null;
        if (pinDialog != null) {
            pinDialog.dismiss();
            pinDialog = null;
        }
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

    /** Apaga as faixas e busca tudo de novo (usado quando o controle parental muda). */
    private void reloadRails() {
        if (!CredentialStore.MODE_XTREAM.equals(creds.mode)) {
            return;
        }
        railGen++;
        focusDone = true; // não tira o foco de onde a pessoa está
        hidePreview();
        LinearLayout[] rails = {railLive, railMovie, railSeries};
        TextView[] states = {stLive, stMovie, stSeries};
        for (int i = 0; i < rails.length; i++) {
            rails[i].removeAllViews();
            states[i].setTextColor(COLOR_NEUTRAL);
            states[i].setText("Carregando...");
            states[i].setVisibility(View.VISIBLE);
        }
        loadRail(XtreamApi.Kind.LIVE, railLive, stLive);
        loadRail(XtreamApi.Kind.MOVIE, railMovie, stMovie);
        loadRail(XtreamApi.Kind.SERIES, railSeries, stSeries);
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
                card.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        schedulePreview(item);
                    }
                });
            } else {
                card.setOnFocusChangeListener(hideOnFocus);
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
        more.setOnFocusChangeListener(hideOnFocus);
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

    // ---------- Controle parental ----------

    private void openParentalMenu() {
        final boolean on = ParentalStore.isEnabled(this);
        AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        if (on) {
            b.setTitle("Controle parental: ativo");
            b.setItems(new CharSequence[]{"Trocar senha", "Desativar controle parental", "Voltar"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                askCurrentPin("Digite a senha atual", new Runnable() {
                                    @Override
                                    public void run() {
                                        createPin();
                                    }
                                });
                            } else if (which == 1) {
                                askCurrentPin("Digite a senha para desativar", new Runnable() {
                                    @Override
                                    public void run() {
                                        ParentalStore.disable(HomeActivity.this);
                                        Toast.makeText(HomeActivity.this,
                                                "Controle parental desativado.",
                                                Toast.LENGTH_SHORT).show();
                                        reloadRails();
                                    }
                                });
                            }
                        }
                    });
        } else {
            b.setTitle("Controle parental: desativado");
            b.setItems(new CharSequence[]{"Ativar controle parental", "Voltar"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                createPin();
                            }
                        }
                    });
        }
        b.show();
    }

    /** Pede a senha nova (4 números). Depois pede para repetir. */
    private void createPin() {
        pinDialog = PinDialog.show(this, "Crie a senha (4 números)", "", new PinDialog.Listener() {
            @Override
            public void onPin(PinDialog d, String pin) {
                d.dismiss();
                confirmPin(pin);
            }

            @Override
            public void onCancel() {
                // saiu sem criar: nada muda
            }
        });
    }

    private void confirmPin(final String first) {
        pinDialog = PinDialog.show(this, "Repita a senha", "", new PinDialog.Listener() {
            @Override
            public void onPin(PinDialog d, String pin) {
                d.dismiss();
                if (!first.equals(pin)) {
                    Toast.makeText(HomeActivity.this,
                            "As senhas não são iguais. Tente de novo.",
                            Toast.LENGTH_SHORT).show();
                    createPin();
                    return;
                }
                if (ParentalStore.enable(HomeActivity.this, pin)) {
                    Toast.makeText(HomeActivity.this,
                            "Controle parental ativado.", Toast.LENGTH_SHORT).show();
                    reloadRails();
                } else {
                    Toast.makeText(HomeActivity.this,
                            "Não foi possível guardar a senha.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancel() {
                // saiu sem confirmar: nada muda
            }
        });
    }

    /** Pede a senha atual. Se estiver certa, fecha o teclado e roda "onOk". */
    private void askCurrentPin(String title, final Runnable onOk) {
        pinDialog = PinDialog.show(this, title, "", new PinDialog.Listener() {
            @Override
            public void onPin(PinDialog d, String pin) {
                long wait = ParentalStore.lockedSeconds(HomeActivity.this);
                if (wait > 0) {
                    d.setMessage("Muitas tentativas. Aguarde " + wait + " segundos.", true);
                    return;
                }
                if (ParentalStore.check(HomeActivity.this, pin)) {
                    d.dismiss();
                    onOk.run();
                    return;
                }
                wait = ParentalStore.lockedSeconds(HomeActivity.this);
                if (wait > 0) {
                    d.setMessage("Muitas tentativas. Aguarde " + wait + " segundos.", true);
                } else {
                    d.setMessage("Senha incorreta.", true);
                }
            }

            @Override
            public void onCancel() {
                // saiu sem digitar: nada muda
            }
        });
    }

    // ---------- Caixinha de prévia ----------

    /** O foco parou num canal: mostra o nome e espera um instante antes de pedir o guia. */
    private void schedulePreview(XtreamApi.Item item) {
        handler.removeCallbacks(previewRunnable);
        previewItem = item;
        tvPvChannel.setText(item.name);
        tvPvTitle.setText("Carregando...");
        tvPvStart.setText("");
        tvPvEnd.setText("");
        tvPvNext.setText("");
        pbPreview.setProgress(0);
        groupPreview.setVisibility(View.VISIBLE);
        handler.postDelayed(previewRunnable, PREVIEW_DELAY_MS);
    }

    private void hidePreview() {
        handler.removeCallbacks(previewRunnable);
        previewItem = null;
        groupPreview.setVisibility(View.INVISIBLE);
    }

    private void fetchPreview(final XtreamApi.Item item) {
        EpgCache.get(creds.server, creds.user, creds.pass, item.id, epg -> {
            if (isFinishing() || isDestroyed() || previewItem != item) {
                return; // o foco já mudou para outro lugar
            }
            showPreview(epg);
        });
    }

    private void showPreview(XtreamApi.Epg epg) {
        if (epg == null) {
            tvPvTitle.setText("Programação indisponível");
            tvPvStart.setText("");
            tvPvEnd.setText("");
            tvPvNext.setText("");
            pbPreview.setProgress(0);
            return;
        }
        tvPvTitle.setText(epg.title);
        tvPvStart.setText(clock(epg.start));
        tvPvEnd.setText(clock(epg.end));

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
        pbPreview.setProgress(percent);

        if (epg.nextTitle == null || epg.nextTitle.isEmpty()) {
            tvPvNext.setText("");
        } else if (epg.nextStart > 0) {
            tvPvNext.setText("A seguir: " + clock(epg.nextStart) + "  " + epg.nextTitle);
        } else {
            tvPvNext.setText("A seguir: " + epg.nextTitle);
        }
    }

    private static String clock(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(millis));
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
            Intent s = new Intent(this, SeriesActivity.class);
            s.putExtra(EXTRA_ID, item.id);
            s.putExtra(EXTRA_NAME, item.name);
            startActivity(s);
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

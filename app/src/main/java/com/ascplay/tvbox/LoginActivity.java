package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class LoginActivity extends Activity {

    private static final int COLOR_NEUTRAL = 0xFFFFFFFF;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    private static final int COLOR_OK = 0xFFC8FF00;

    private Button btnXtream;
    private Button btnM3u;
    private Button btnEnter;
    private View groupXtream;
    private View groupM3u;
    private EditText etServer;
    private EditText etUser;
    private EditText etPass;
    private EditText etM3u;
    private TextView tvStatus;
    private ScrollView scrollLogin;
    private View spaceKeyboard;

    // Aviso de atualização
    private View groupUpdate;
    private TextView tvUpdate;
    private Button btnUpdateNow;
    private Button btnUpdateLater;
    private Updater.Info pendingUpdate;
    private boolean downloading = false;

    // Código do aparelho (ligação com o app de gerenciar)
    private static final long POLL_MS = 8000;
    private TextView tvCode;
    private TextView tvLinkStatus;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pollTask = this::pollNow;
    private volatile boolean resumed = false;
    private boolean linkScreen = false;
    private boolean forceLogin = false;
    private boolean pollRunning = false;

    private String mode = CredentialStore.MODE_XTREAM;
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Já existe login salvo? Vai direto para a página inicial.
        // (O botão "Trocar login" da página inicial pede para mostrar esta tela.)
        CredentialStore.Credentials saved = CredentialStore.load(this);
        forceLogin = getIntent().getBooleanExtra(HomeActivity.EXTRA_SHOW_LOGIN, false);
        if (saved != null && !forceLogin) {
            // Confere em segundo plano se o dono mandou uma lista nova.
            DeviceLink.syncInBackground(this);
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        btnXtream = findViewById(R.id.btn_mode_xtream);
        btnM3u = findViewById(R.id.btn_mode_m3u);
        btnEnter = findViewById(R.id.btn_enter);
        groupXtream = findViewById(R.id.group_xtream);
        groupM3u = findViewById(R.id.group_m3u);
        etServer = findViewById(R.id.et_server);
        etUser = findViewById(R.id.et_user);
        etPass = findViewById(R.id.et_pass);
        etM3u = findViewById(R.id.et_m3u);
        tvStatus = findViewById(R.id.tv_status);
        scrollLogin = findViewById(R.id.scroll_login);
        spaceKeyboard = findViewById(R.id.space_keyboard);
        tvCode = findViewById(R.id.tv_code);
        tvLinkStatus = findViewById(R.id.tv_link_status);
        linkScreen = true;

        groupUpdate = findViewById(R.id.group_update);
        tvUpdate = findViewById(R.id.tv_update);
        btnUpdateNow = findViewById(R.id.btn_update_now);
        btnUpdateLater = findViewById(R.id.btn_update_later);

        btnXtream.setOnClickListener(v -> setMode(CredentialStore.MODE_XTREAM));
        btnM3u.setOnClickListener(v -> setMode(CredentialStore.MODE_M3U));
        btnEnter.setOnClickListener(v -> attemptLogin());
        btnUpdateNow.setOnClickListener(v -> startUpdate());
        btnUpdateLater.setOnClickListener(v -> dismissUpdate());

        TextView.OnEditorActionListener enterOnDone = (v, actionId, event) -> {
            boolean done = actionId == EditorInfo.IME_ACTION_DONE;
            boolean enterKey = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (done || enterKey) {
                attemptLogin();
                return true;
            }
            return false;
        };
        etPass.setOnEditorActionListener(enterOnDone);
        etM3u.setOnEditorActionListener(enterOnDone);

        // Teclado aberto: o campo sobe para o alto da tela, acima do teclado
        View.OnFocusChangeListener keepFieldVisible = (v, hasFocus) -> {
            if (hasFocus) {
                spaceKeyboard.setVisibility(View.VISIBLE);
                scrollFieldToTop(v);
            } else {
                // Só tira o espaço se o foco não foi para outro campo de texto
                scrollLogin.post(() -> {
                    if (!etServer.hasFocus() && !etUser.hasFocus()
                            && !etPass.hasFocus() && !etM3u.hasFocus()) {
                        spaceKeyboard.setVisibility(View.GONE);
                    }
                });
            }
        };
        etServer.setOnFocusChangeListener(keepFieldVisible);
        etUser.setOnFocusChangeListener(keepFieldVisible);
        etPass.setOnFocusChangeListener(keepFieldVisible);
        etM3u.setOnFocusChangeListener(keepFieldVisible);

        // Preenche com o último login salvo (se houver).
        if (saved != null) {
            etServer.setText(saved.server);
            etUser.setText(saved.user);
            etPass.setText(saved.pass);
            etM3u.setText(saved.m3uUrl);
            mode = CredentialStore.MODE_M3U.equals(saved.mode)
                    ? CredentialStore.MODE_M3U
                    : CredentialStore.MODE_XTREAM;
        }
        setMode(mode);

        checkForUpdate();
    }

    /** Rola a tela para o campo ficar no alto, longe do teclado. Espera o teclado abrir. */
    private void scrollFieldToTop(final View field) {
        Runnable doScroll = () -> {
            int top = 0;
            View v = field;
            while (v != null && v != scrollLogin) {
                top += v.getTop();
                if (!(v.getParent() instanceof View)) {
                    break;
                }
                v = (View) v.getParent();
            }
            int margin = (int) (60 * getResources().getDisplayMetrics().density);
            scrollLogin.smoothScrollTo(0, Math.max(0, top - margin));
        };
        scrollLogin.postDelayed(doScroll, 150);
        scrollLogin.postDelayed(doScroll, 450);
    }

    // ---------- Código do aparelho ----------

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (!linkScreen) {
            return;
        }
        // Se a lista chegou enquanto o app estava parado, entra direto.
        if (!forceLogin && CredentialStore.load(this) != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }
        handler.removeCallbacks(pollTask);
        handler.post(pollTask);
    }

    @Override
    protected void onPause() {
        resumed = false;
        handler.removeCallbacks(pollTask);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(pollTask);
        super.onDestroy();
    }

    private void pollNow() {
        if (!resumed || pollRunning || isFinishing() || isDestroyed()) {
            return;
        }
        pollRunning = true;
        final Context app = getApplicationContext();
        new Thread(() -> {
            DeviceLink.Reply reply;
            boolean taken = false;
            String code = "";
            try {
                // Mostra o código logo, mesmo sem internet.
                code = DeviceLink.getDisplayCode(app);
                final String first = code;
                runOnUiThread(() -> showCode(first));
                reply = DeviceLink.poll(app);
                code = DeviceLink.getDisplayCode(app);
                if (reply.state == DeviceLink.State.READY && resumed
                        && (CredentialStore.load(app) == null || DeviceLink.isNewer(app, reply))) {
                    DeviceLink.apply(app, reply);
                    taken = true;
                }
            } catch (Exception e) {
                reply = new DeviceLink.Reply(DeviceLink.State.OFFLINE, 0, null);
            }
            final DeviceLink.Reply r = reply;
            final boolean didTake = taken;
            final String finalCode = code;
            runOnUiThread(() -> onLinkReply(r, didTake, finalCode));
        }).start();
    }

    private void showCode(String code) {
        if (isFinishing() || isDestroyed() || tvCode == null) {
            return;
        }
        tvCode.setText(code);
    }

    private void onLinkReply(DeviceLink.Reply r, boolean taken, String code) {
        pollRunning = false;
        if (isFinishing() || isDestroyed()) {
            return;
        }
        showCode(code);
        if (taken) {
            tvLinkStatus.setTextColor(COLOR_OK);
            tvLinkStatus.setText(R.string.link_received);
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }
        tvLinkStatus.setTextColor(0xFFA8A8A8);
        if (r.state == DeviceLink.State.WAITING) {
            tvLinkStatus.setText(R.string.link_waiting);
        } else if (r.state == DeviceLink.State.OFFLINE) {
            tvLinkStatus.setText(R.string.link_offline);
        } else {
            tvLinkStatus.setText("");
        }
        if (resumed) {
            handler.removeCallbacks(pollTask);
            handler.postDelayed(pollTask, POLL_MS);
        }
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
        tvUpdate.setTextColor(COLOR_NEUTRAL);
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
        btnXtream.requestFocus();
    }

    // ---------- Login ----------

    private void setMode(String newMode) {
        mode = newMode;
        boolean xtream = CredentialStore.MODE_XTREAM.equals(mode);
        groupXtream.setVisibility(xtream ? View.VISIBLE : View.GONE);
        groupM3u.setVisibility(xtream ? View.GONE : View.VISIBLE);
        btnXtream.setText((xtream ? "\u25CF " : "") + getString(R.string.mode_xtream));
        btnM3u.setText((xtream ? "" : "\u25CF ") + getString(R.string.mode_m3u));
        tvStatus.setText("");
    }

    private void attemptLogin() {
        if (busy) {
            return;
        }
        final String currentMode = mode;
        final String server = etServer.getText().toString();
        final String user = etUser.getText().toString();
        final String pass = etPass.getText().toString();
        final String m3u = etM3u.getText().toString();

        busy = true;
        btnEnter.setEnabled(false);
        showStatus(getString(R.string.msg_checking), COLOR_NEUTRAL);
        hideKeyboard();

        new Thread(() -> {
            LoginChecker.Result r;
            if (CredentialStore.MODE_XTREAM.equals(currentMode)) {
                r = LoginChecker.checkXtream(server, user, pass);
            } else {
                r = LoginChecker.checkM3u(m3u);
            }
            final LoginChecker.Result result = r;
            runOnUiThread(() -> onResult(result, currentMode, server, user, pass, m3u));
        }).start();
    }

    private void onResult(LoginChecker.Result result, String usedMode,
                          String server, String user, String pass, String m3u) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        busy = false;
        btnEnter.setEnabled(true);

        if (result == LoginChecker.Result.OK) {
            CredentialStore.Credentials c;
            if (CredentialStore.MODE_XTREAM.equals(usedMode)) {
                String origin = LoginChecker.normalizeServer(server);
                c = new CredentialStore.Credentials(usedMode,
                        origin == null ? server.trim() : origin,
                        user.trim(), pass.trim(), "");
            } else {
                c = new CredentialStore.Credentials(usedMode, "", "", "", m3u.trim());
            }
            CredentialStore.save(this, c);
            showStatus(getString(R.string.msg_ok), COLOR_OK);
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        showStatus(getString(messageFor(result)), COLOR_ERROR);
        btnEnter.requestFocus();
    }

    private int messageFor(LoginChecker.Result result) {
        switch (result) {
            case EMPTY:
                return R.string.err_empty;
            case BAD_SERVER:
                return R.string.err_server;
            case BAD_URL:
                return R.string.err_url;
            case NETWORK:
                return R.string.err_network;
            case AUTH:
                return R.string.err_auth;
            case EXPIRED:
                return R.string.err_expired;
            case NOT_M3U:
                return R.string.err_not_m3u;
            default:
                return R.string.err_unknown;
        }
    }

    private void showStatus(String text, int color) {
        tvStatus.setTextColor(color);
        tvStatus.setText(text);
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}
